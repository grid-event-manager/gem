package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequester
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequestResult
import org.hostess.protocol.libomv.transport.CapabilityName
import org.hostess.protocol.libomv.transport.CapabilityUrlProvider
import org.hostess.protocol.libomv.transport.CapabilityUrlResult
import org.hostess.protocol.libomv.transport.EventQueueGetResult
import org.hostess.protocol.libomv.transport.EventQueueGetSource

internal class ProtocolCurrentGroupsSource(
    private val capabilityUrlProvider: CapabilityUrlProvider,
    private val eventQueueGetClient: EventQueueGetSource,
    private val requestTransport: AgentDataUpdateRequester,
) : CurrentGroupsSource {
    override fun currentGroups(identity: LibomvSessionIdentity): CurrentGroupsFetchResult {
        val eventQueueUrl = when (val capability = capabilityUrlProvider.requireUrl(
            identity,
            CapabilityName.EVENT_QUEUE_GET,
        )) {
            is CapabilityUrlResult.Ready -> capability.url
            is CapabilityUrlResult.TransportGap -> return transportGap(capability.redactedMessage)
            is CapabilityUrlResult.MappingGap -> return mappingGap(capability.redactedMessage)
        }

        when (val sent = requestTransport.send(identity)) {
            AgentDataUpdateRequestResult.Sent -> Unit
            is AgentDataUpdateRequestResult.Failed -> return CurrentGroupsFetchResult.Failure(
                status = CurrentGroupsFailureStatus.PACKET_GAP,
                redactedMessage = "current groups transport packet failed: ${sent.redactedMessage}",
            )
        }

        return when (val polled = eventQueueGetClient.pollAgentGroupDataUpdate(eventQueueUrl)) {
            is EventQueueGetResult.AgentGroupDataUpdate -> CurrentGroupsFetchResult.Success(polled.groups)
            EventQueueGetResult.TimedOut -> proofGap()
            is EventQueueGetResult.TransportGap -> transportGap(polled.redactedMessage)
            is EventQueueGetResult.MappingGap -> mappingGap(polled.redactedMessage)
        }
    }

    private fun transportGap(message: String = "current groups transport unavailable"): CurrentGroupsFetchResult.Failure =
        CurrentGroupsFetchResult.Failure(
            status = CurrentGroupsFailureStatus.TRANSPORT_GAP,
            redactedMessage = message,
        )

    private fun mappingGap(message: String = "current groups event invalid"): CurrentGroupsFetchResult.Failure =
        CurrentGroupsFetchResult.Failure(
            status = CurrentGroupsFailureStatus.PROOF_GAP,
            redactedMessage = message,
        )

    private fun proofGap(): CurrentGroupsFetchResult.Failure = CurrentGroupsFetchResult.Failure(
        status = CurrentGroupsFailureStatus.PROOF_GAP,
        redactedMessage = "current groups event unavailable",
    )
}
