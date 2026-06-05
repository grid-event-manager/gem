package org.hostess.protocol.libomv.transport

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

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
    private val datagramSender: SimulatorDatagramSender = UdpSimulatorDatagramSender(),
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
            datagramSender.send(endpoint, packets)
            SimulatorCircuitSendResult.Sent
        } catch (ex: IOException) {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        } catch (ex: IllegalArgumentException) {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        } catch (ex: SecurityException) {
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

internal fun interface SimulatorDatagramSender {
    @Throws(IOException::class)
    fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>)
}

private class UdpSimulatorDatagramSender : SimulatorDatagramSender {
    override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
        val address = InetAddress.getByName(endpoint.host)
        DatagramSocket().use { socket ->
            payloads.forEach { payload ->
                socket.send(DatagramPacket(payload, payload.size, address, endpoint.port))
            }
        }
    }
}
