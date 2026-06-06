package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AgentDataUpdateRequestTransportTest {
    @Test
    fun `passes internal identity to bounded circuit sender`() {
        val sender = RecordingCircuitSender(SimulatorCircuitSendResult.Sent)
        val transport = AgentDataUpdateRequestTransport(sender)

        val result = transport.send(identity())

        assertEquals(AgentDataUpdateRequestResult.Sent, result)
        assertEquals(
            SimulatorCircuit(
                agentId = AGENT_ID,
                sessionId = SESSION_ID,
                seedCapability = "seed-capability",
                simulatorIp = SIM_HOST,
                simulatorPort = SIM_PORT,
                regionHandle = REGION_HANDLE,
                circuitCode = CIRCUIT_CODE,
            ),
            sender.capturedCircuit,
        )
    }

    @Test
    fun `maps bounded circuit failure to redacted request failure`() {
        val transport = AgentDataUpdateRequestTransport(
            RecordingCircuitSender(SimulatorCircuitSendResult.Failed("bounded simulator send failed")),
        )

        val result = assertIs<AgentDataUpdateRequestResult.Failed>(transport.send(identity()))

        assertEquals("bounded simulator send failed", result.redactedMessage)
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

    private class RecordingCircuitSender(
        private val result: SimulatorCircuitSendResult,
    ) : BoundedSimulatorCircuitSender {
        var capturedCircuit: SimulatorCircuit? = null

        override fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult {
            capturedCircuit = circuit
            return result
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
