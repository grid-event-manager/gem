package org.hostess.protocol.libomv.transport

internal data class SimulatorCircuit(
    val agentId: String,
    val sessionId: String,
    val seedCapability: String,
    val simulatorIp: String,
    val simulatorPort: Int,
    val regionHandle: Long,
    val circuitCode: Long,
)

internal fun interface BoundedSimulatorCircuitSender {
    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult
}

internal sealed interface SimulatorCircuitSendResult {
    data object Sent : SimulatorCircuitSendResult
    data class Failed(val redactedMessage: String) : SimulatorCircuitSendResult
}

internal class BoundedSimulatorCircuitClient(
    private val packetSender: SimulatorPacketSender,
    private val sequence: SimulatorPacketSequence = SimulatorPacketSequence(),
) : BoundedSimulatorCircuitSender {
    override fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult {
        if (!circuit.isUsable()) {
            return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        val packets = try {
            listOf(
                LibomvPacketCodec.useCircuitCode(circuit, sequence.next()),
                LibomvPacketCodec.completeAgentMovement(circuit, sequence.next()),
                LibomvPacketCodec.agentDataUpdateRequest(circuit, sequence.next()),
            )
        } catch (ex: IllegalArgumentException) {
            return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }

        return try {
            packetSender.send(endpoint, packets)
            SimulatorCircuitSendResult.Sent
        } catch (ex: Exception) {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }
    }

    private fun SimulatorCircuit.isUsable(): Boolean =
        agentId.isNotBlank() &&
            sessionId.isNotBlank() &&
            seedCapability.isNotBlank() &&
            simulatorIp.isNotBlank() &&
            simulatorPort in 1..MAX_PORT &&
            circuitCode in 1..UNSIGNED_32_MAX

    private companion object {
        const val REDACTED_SEND_FAILURE: String = "bounded simulator send failed"
        const val MAX_PORT: Int = 65535
        const val UNSIGNED_32_MAX: Long = 0xFFFF_FFFFL
    }
}

internal data class SimulatorEndpoint(
    val host: String,
    val port: Int,
)

internal fun interface SimulatorPacketSender {
    fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>)
}
