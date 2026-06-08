package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AgentDataUpdateRequestTransportTest {
    @Test
    fun `passes internal identity to protocol circuit client`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(exchange, SimulatorPacketSequence(0)),
        )

        val result = transport.send(identity())

        assertEquals(AgentDataUpdateRequestResult.Sent, result)
        assertEquals(SIM_HOST, exchange.endpoint?.host)
        assertEquals(SIM_PORT, exchange.endpoint?.port)
        assertEquals(5, exchange.payloads.size)
    }

    @Test
    fun `maps protocol circuit failure to redacted request failure`() {
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(
                RecordingPacketExchange(failure = Exception("cannot reach $SIM_HOST with $AGENT_ID")),
            ),
        )

        val result = assertIs<AgentDataUpdateRequestResult.Failed>(transport.send(identity()))

        assertEquals("protocol simulator send failed", result.redactedMessage)
    }

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = SIM_HOST,
        simulatorPort = SIM_PORT,
        regionHandle = REGION_HANDLE,
        circuitCode = CIRCUIT_CODE,
    )

    private fun regionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 101, packetId = 148, flags = 0xC0),
        )

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private class RecordingPacketExchange(
        private val inboundPayloads: MutableList<ByteArray> = mutableListOf(),
        private val failure: Exception? = null,
    ) : SimulatorPacketExchange {
        var endpoint: SimulatorEndpoint? = null
        var payloads: List<ByteArray> = emptyList()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            this.endpoint = endpoint
            this.payloads = this.payloads + payloads
        }

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            if (inboundPayloads.isEmpty()) {
                null
            } else {
                SimulatorInboundPacket(endpoint, inboundPayloads.removeAt(0))
            }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val REGION_HANDLE = 123456789L
        const val CIRCUIT_CODE = 0x01020304L
    }
}
