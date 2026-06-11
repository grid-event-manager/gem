package org.hostess.protocol.libomv.runtime

import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.RecordingSimulatorSessionGateway
import org.hostess.protocol.libomv.transport.SimulatorPresenceResult
import org.hostess.protocol.libomv.transport.SimulatorPresenceStatus
import org.hostess.protocol.libomv.transport.SimulatorSessionHealth
import org.hostess.protocol.libomv.transport.SimulatorSessionHealthStatus
import org.hostess.protocol.libomv.transport.toSimulatorCircuit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolSimulatorPresenceSourceTest {
    @Test
    fun `maps circuit presence success to passed proof fields`() {
        val gateway = RecordingSimulatorSessionGateway(
            presenceResult = SimulatorPresenceResult.Present(
                pingReplies = 1,
                cached = false,
                heartbeatActive = true,
            ),
        )
        val source = ProtocolSimulatorPresenceSource(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val proof = assertIs<SimulatorPresenceProofResult.Success>(
            source.simulatorPresence(identity()),
        ).proof

        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.regionHandshakeStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.regionHandshakeReplyStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.agentMovementStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.agentUpdateStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.heartbeatStatus)
        assertEquals(1, proof.pingReplies)
        assertEquals(identity().toSimulatorCircuit(), gateway.presenceCircuits.single())
    }

    @Test
    fun `maps stale heartbeat to proof gap without failing base presence fields`() {
        val gateway = RecordingSimulatorSessionGateway(
            presenceResult = SimulatorPresenceResult.Present(
                pingReplies = 2,
                cached = false,
                heartbeatActive = true,
                sessionHealth = SimulatorSessionHealth(SimulatorSessionHealthStatus.STALE),
            ),
        )
        val source = ProtocolSimulatorPresenceSource(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val proof = assertIs<SimulatorPresenceProofResult.Success>(
            source.simulatorPresence(identity()),
        ).proof

        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PROOF_GAP, proof.heartbeatStatus)
        assertEquals("simulator heartbeat proof_gap", proof.redactedMessage)
        assertEquals(2, proof.pingReplies)
    }

    @Test
    fun `maps movement timeout to proof gap without claiming agent update`() {
        val gateway = RecordingSimulatorSessionGateway(
            presenceResult = SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
                redactedMessage = "protocol simulator send failed",
            ),
        )
        val source = ProtocolSimulatorPresenceSource(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val failure = assertIs<SimulatorPresenceProofResult.Failure>(
            source.simulatorPresence(identity()),
        )

        assertEquals(SimulatorPresenceProofStatus.PROOF_GAP, failure.proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, failure.proof.regionHandshakeStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, failure.proof.regionHandshakeReplyStatus)
        assertEquals(SimulatorPresenceProofStatus.PROOF_GAP, failure.proof.agentMovementStatus)
        assertEquals(SimulatorPresenceProofStatus.NOT_RUN, failure.proof.agentUpdateStatus)
        assertEquals(SimulatorPresenceProofStatus.NOT_RUN, failure.proof.heartbeatStatus)
        assertEquals("simulator presence proof_gap", failure.failure.redactedMessage)
    }

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = SIM_HOST,
        simulatorPort = SIM_PORT,
        regionHandle = 123456789L,
        circuitCode = CIRCUIT_CODE,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val CIRCUIT_CODE = 0x01020304L
    }
}
