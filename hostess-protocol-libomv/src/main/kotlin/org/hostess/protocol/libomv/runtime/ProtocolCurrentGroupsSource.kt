package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequester
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequestResult
import org.hostess.protocol.libomv.transport.EventQueueGetResult
import org.hostess.protocol.libomv.transport.EventQueueGetSource

internal class ProtocolCurrentGroupsSource(
    private val eventQueueGetClient: EventQueueGetSource,
    private val requestTransport: AgentDataUpdateRequester,
) : CurrentGroupsSource {
    override fun currentGroups(identity: LibomvSessionIdentity): CurrentGroupsFetchResult {
        val eventQueueUrl = when (val seed = eventQueueGetClient.seed(identity.seedCapability)) {
            is EventQueueGetResult.Ready -> seed.eventQueueUrl
            EventQueueGetResult.TransportGap -> return transportGap()
            EventQueueGetResult.MappingGap -> return mappingGap()
            EventQueueGetResult.TimedOut -> return proofGap()
            is EventQueueGetResult.AgentGroupDataUpdate -> return CurrentGroupsFetchResult.Success(seed.groups)
        }

        when (val sent = requestTransport.send(identity)) {
            AgentDataUpdateRequestResult.Sent -> Unit
            is AgentDataUpdateRequestResult.Failed -> return CurrentGroupsFetchResult.Failure(
                status = CurrentGroupsFailureStatus.PACKET_GAP,
                redactedMessage = sent.redactedMessage,
            )
        }

        return when (val polled = eventQueueGetClient.pollAgentGroupDataUpdate(eventQueueUrl)) {
            is EventQueueGetResult.AgentGroupDataUpdate -> CurrentGroupsFetchResult.Success(polled.groups)
            EventQueueGetResult.TimedOut -> proofGap()
            EventQueueGetResult.TransportGap -> transportGap()
            EventQueueGetResult.MappingGap -> mappingGap()
            is EventQueueGetResult.Ready -> mappingGap()
        }
    }

    private fun transportGap(): CurrentGroupsFetchResult.Failure = CurrentGroupsFetchResult.Failure(
        status = CurrentGroupsFailureStatus.TRANSPORT_GAP,
        redactedMessage = "current groups transport unavailable",
    )

    private fun mappingGap(): CurrentGroupsFetchResult.Failure = CurrentGroupsFetchResult.Failure(
        status = CurrentGroupsFailureStatus.PROOF_GAP,
        redactedMessage = "current groups event invalid",
    )

    private fun proofGap(): CurrentGroupsFetchResult.Failure = CurrentGroupsFetchResult.Failure(
        status = CurrentGroupsFailureStatus.PROOF_GAP,
        redactedMessage = "current groups event unavailable",
    )
}
