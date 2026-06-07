package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AgentDataUpdateRequestTransportTest {
    @Test
    fun `passes internal identity to protocol circuit client`() {
        val sender = RecordingPacketSender()
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(sender, SimulatorPacketSequence(0)),
        )

        val result = transport.send(identity())

        assertEquals(AgentDataUpdateRequestResult.Sent, result)
        assertEquals(SIM_HOST, sender.endpoint?.host)
        assertEquals(SIM_PORT, sender.endpoint?.port)
        assertEquals(3, sender.payloads.size)
    }

    @Test
    fun `maps protocol circuit failure to redacted request failure`() {
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(
                RecordingPacketSender(failure = Exception("cannot reach $SIM_HOST with $AGENT_ID")),
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

    private class RecordingPacketSender(
        private val failure: Exception? = null,
    ) : SimulatorPacketSender {
        var endpoint: SimulatorEndpoint? = null
        var payloads: List<ByteArray> = emptyList()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            this.endpoint = endpoint
            this.payloads = payloads
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
