package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvSessionIdentity

internal fun interface AgentDataUpdateRequester {
    fun send(identity: LibomvSessionIdentity): AgentDataUpdateRequestResult
}

internal class AgentDataUpdateRequestTransport(
    private val circuitSender: BoundedSimulatorCircuitSender,
) : AgentDataUpdateRequester {
    override fun send(identity: LibomvSessionIdentity): AgentDataUpdateRequestResult {
        val circuit = SimulatorCircuit(
            agentId = identity.agentId,
            sessionId = identity.sessionId,
            seedCapability = identity.seedCapability,
            simulatorIp = identity.simulatorIp,
            simulatorPort = identity.simulatorPort,
            regionHandle = identity.regionHandle,
            circuitCode = identity.circuitCode,
        )
        return when (val result = circuitSender.sendCurrentGroupsRequest(circuit)) {
            SimulatorCircuitSendResult.Sent -> AgentDataUpdateRequestResult.Sent
            is SimulatorCircuitSendResult.Failed -> AgentDataUpdateRequestResult.Failed(result.redactedMessage)
        }
    }
}

internal sealed interface AgentDataUpdateRequestResult {
    data object Sent : AgentDataUpdateRequestResult
    data class Failed(val redactedMessage: String) : AgentDataUpdateRequestResult
}
