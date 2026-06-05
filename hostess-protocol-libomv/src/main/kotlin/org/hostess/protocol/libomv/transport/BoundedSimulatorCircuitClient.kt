package org.hostess.protocol.libomv.transport

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

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
    private val sequence: AtomicInteger = AtomicInteger(),
) : BoundedSimulatorCircuitSender {
    override fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult {
        if (!circuit.isUsable()) {
            return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        val packets = try {
            listOf(
                PacketCodec.useCircuitCode(circuit, sequence.incrementAndGet()),
                PacketCodec.completeAgentMovement(circuit, sequence.incrementAndGet()),
                PacketCodec.agentDataUpdateRequest(circuit, sequence.incrementAndGet()),
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

private object PacketCodec {
    fun useCircuitCode(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = USE_CIRCUIT_CODE,
        sequence = sequence,
        bodyLength = U32_BYTES + UUID_BYTES + UUID_BYTES,
    ) {
        putU32(circuit.circuitCode)
        putUuid(circuit.sessionId)
        putUuid(circuit.agentId)
    }

    fun completeAgentMovement(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = COMPLETE_AGENT_MOVEMENT,
        sequence = sequence,
        bodyLength = UUID_BYTES + UUID_BYTES + U32_BYTES,
    ) {
        putUuid(circuit.agentId)
        putUuid(circuit.sessionId)
        putU32(circuit.circuitCode)
    }

    fun agentDataUpdateRequest(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = AGENT_DATA_UPDATE_REQUEST,
        sequence = sequence,
        bodyLength = UUID_BYTES + UUID_BYTES,
    ) {
        putUuid(circuit.agentId)
        putUuid(circuit.sessionId)
    }

    private fun lowPacket(
        packetId: Int,
        sequence: Int,
        bodyLength: Int,
        body: ByteBuffer.() -> Unit,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(LOW_HEADER_BYTES + bodyLength)
        buffer.put(0.toByte())
        buffer.putInt(sequence)
        buffer.put(0.toByte())
        buffer.put(LOW_FREQUENCY_MARKER)
        buffer.put(LOW_FREQUENCY_MARKER)
        buffer.put(((packetId ushr 8) and BYTE_MASK).toByte())
        buffer.put((packetId and BYTE_MASK).toByte())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.body()
        return buffer.array()
    }

    private fun ByteBuffer.putU32(value: Long) {
        require(value in 1..UNSIGNED_32_MAX)
        putInt(value.toInt())
    }

    private fun ByteBuffer.putUuid(value: String) {
        put(UUID.fromString(value).toPacketBytes())
    }

    private fun UUID.toPacketBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(UUID_BYTES)
        buffer.putLong(mostSignificantBits)
        buffer.putLong(leastSignificantBits)
        return buffer.array()
    }

    private const val USE_CIRCUIT_CODE = 3
    private const val COMPLETE_AGENT_MOVEMENT = 249
    private const val AGENT_DATA_UPDATE_REQUEST = 386
    private const val LOW_HEADER_BYTES = 10
    private const val UUID_BYTES = 16
    private const val U32_BYTES = 4
    private const val LOW_FREQUENCY_MARKER: Byte = 0xFF.toByte()
    private const val BYTE_MASK = 0xFF
    private const val UNSIGNED_32_MAX = 0xFFFF_FFFFL
}
