package org.gem.protocol.libomv.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.protocol.libomv.LibomvSessionIdentity

class AgentDataUpdateRequestTransportTest {
    @Test
    fun `passes internal identity to protocol circuit client`() {
        val gateway = RecordingSimulatorSessionGateway()
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val result = transport.send(identity())

        assertEquals(AgentDataUpdateRequestResult.Sent, result)
        assertEquals(identity().toSimulatorCircuit(), gateway.currentGroupsCircuits.single())
    }

    @Test
    fun `maps protocol circuit failure to redacted request failure`() {
        val gateway = RecordingSimulatorSessionGateway(
            currentGroupsResult = SimulatorCircuitSendResult.Failed("protocol simulator send failed"),
        )
        val transport = AgentDataUpdateRequestTransport(
            ProtocolSimulatorCircuitClient(gateway),
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

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val REGION_HANDLE = 123456789L
        const val CIRCUIT_CODE = 0x01020304L
    }
}
