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

internal sealed interface SimulatorCircuitSendResult {
    data object Sent : SimulatorCircuitSendResult
    data class Failed(val redactedMessage: String) : SimulatorCircuitSendResult
}

internal class ProtocolSimulatorCircuitClient(
    private val packetSender: SimulatorPacketSender,
    private val sequence: SimulatorPacketSequence = SimulatorPacketSequence(),
) {
    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
        send(circuit) {
            listOf(
                LibomvPacketCodec.useCircuitCode(circuit, sequence.next()),
                LibomvPacketCodec.completeAgentMovement(circuit, sequence.next()),
                LibomvPacketCodec.agentDataUpdateRequest(circuit, sequence.next()),
            )
        }

    private fun send(
        circuit: SimulatorCircuit,
        payloads: () -> List<ByteArray>,
    ): SimulatorCircuitSendResult {
        if (!circuit.isUsable()) {
            return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        val packetPayloads = try {
            payloads()
        } catch (ex: IllegalArgumentException) {
            return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }

        return try {
            packetSender.send(endpoint, packetPayloads)
            SimulatorCircuitSendResult.Sent
        } catch (ex: Exception) {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }
    }

    private fun SimulatorCircuit.isUsable(): Boolean =
        agentId.isNotBlank() &&
            sessionId.isNotBlank() &&
            seedCapability.isNotBlank() &&
            simulatorIp.isIpv4Address() &&
            simulatorPort in 1..MAX_PORT &&
            circuitCode in 1..UNSIGNED_32_MAX

    private fun String.isIpv4Address(): Boolean {
        val parts = split(".")
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                part.all(Char::isDigit) &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private companion object {
        const val REDACTED_SEND_FAILURE: String = "protocol simulator send failed"
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
