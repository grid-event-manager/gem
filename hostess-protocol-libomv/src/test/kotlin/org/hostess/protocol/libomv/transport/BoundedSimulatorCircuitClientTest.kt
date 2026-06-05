package org.hostess.protocol.libomv.transport

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class BoundedSimulatorCircuitClientTest {
    @Test
    fun `sends only bounded current-groups packet sequence`() {
        val sender = RecordingDatagramSender()
        val client = BoundedSimulatorCircuitClient(
            datagramSender = sender,
            sequence = AtomicInteger(0),
        )

        val result = client.sendCurrentGroupsRequest(circuit())

        assertEquals(SimulatorCircuitSendResult.Sent, result)
        assertEquals(1, sender.sent.size)
        assertEquals(SIM_HOST, sender.sent.single().endpoint.host)
        assertEquals(SIM_PORT, sender.sent.single().endpoint.port)
        val payloads = sender.sent.single().payloads
        assertEquals(3, payloads.size)
        assertLowPacket(
            payload = payloads[0],
            sequence = 1,
            packetId = 3,
            body = u32(CIRCUIT_CODE) + uuid(SESSION_ID) + uuid(AGENT_ID),
        )
        assertLowPacket(
            payload = payloads[1],
            sequence = 2,
            packetId = 249,
            body = uuid(AGENT_ID) + uuid(SESSION_ID) + u32(CIRCUIT_CODE),
        )
        assertLowPacket(
            payload = payloads[2],
            sequence = 3,
            packetId = 386,
            body = uuid(AGENT_ID) + uuid(SESSION_ID),
        )
    }

    @Test
    fun `send failures are redacted`() {
        val sender = RecordingDatagramSender(
            failure = IOException("cannot reach $SIM_HOST:$SIM_PORT for $AGENT_ID"),
        )
        val client = BoundedSimulatorCircuitClient(datagramSender = sender)

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            client.sendCurrentGroupsRequest(circuit()),
        )

        assertEquals("bounded simulator send failed", result.redactedMessage)
        assertFalse(result.redactedMessage.contains(SIM_HOST))
        assertFalse(result.redactedMessage.contains(AGENT_ID))
        assertFalse(result.redactedMessage.contains(CIRCUIT_CODE.toString()))
    }

    @Test
    fun `invalid identity fails before datagram send`() {
        val sender = RecordingDatagramSender()
        val client = BoundedSimulatorCircuitClient(datagramSender = sender)

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            client.sendCurrentGroupsRequest(circuit(agentId = "not-a-uuid")),
        )

        assertEquals("bounded simulator send failed", result.redactedMessage)
        assertEquals(0, sender.sent.size)
    }

    private fun assertLowPacket(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        body: ByteArray,
    ) {
        val header = byteArrayOf(
            0x00,
            ((sequence ushr 24) and 0xFF).toByte(),
            ((sequence ushr 16) and 0xFF).toByte(),
            ((sequence ushr 8) and 0xFF).toByte(),
            (sequence and 0xFF).toByte(),
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            ((packetId ushr 8) and 0xFF).toByte(),
            (packetId and 0xFF).toByte(),
        )
        assertContentEquals(header + body, payload)
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun uuid(value: String): ByteArray =
        value.replace("-", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    private fun circuit(
        agentId: String = AGENT_ID,
        sessionId: String = SESSION_ID,
    ): SimulatorCircuit = SimulatorCircuit(
        agentId = agentId,
        sessionId = sessionId,
        seedCapability = "seed-capability",
        simulatorIp = SIM_HOST,
        simulatorPort = SIM_PORT,
        regionHandle = 123456789L,
        circuitCode = CIRCUIT_CODE,
    )

    private class RecordingDatagramSender(
        private val failure: IOException? = null,
    ) : SimulatorDatagramSender {
        val sent = mutableListOf<SentDatagram>()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            sent += SentDatagram(endpoint, payloads)
        }
    }

    private data class SentDatagram(
        val endpoint: SimulatorEndpoint,
        val payloads: List<ByteArray>,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val CIRCUIT_CODE = 0x01020304L
    }
}
