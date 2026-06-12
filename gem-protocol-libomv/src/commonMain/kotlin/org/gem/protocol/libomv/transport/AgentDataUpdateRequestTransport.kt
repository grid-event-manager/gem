package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.LibomvSessionIdentity

internal fun interface AgentDataUpdateRequester {
    fun send(identity: LibomvSessionIdentity): AgentDataUpdateRequestResult
}

internal class AgentDataUpdateRequestTransport(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : AgentDataUpdateRequester {
    override fun send(identity: LibomvSessionIdentity): AgentDataUpdateRequestResult {
        return when (val result = circuitClient.sendCurrentGroupsRequest(identity.toSimulatorCircuit())) {
            is SimulatorCircuitSendResult.Sent -> AgentDataUpdateRequestResult.Sent
            is SimulatorCircuitSendResult.Failed -> AgentDataUpdateRequestResult.Failed(result.redactedMessage)
        }
    }
}

internal sealed interface AgentDataUpdateRequestResult {
    data object Sent : AgentDataUpdateRequestResult
    data class Failed(val redactedMessage: String) : AgentDataUpdateRequestResult
}
