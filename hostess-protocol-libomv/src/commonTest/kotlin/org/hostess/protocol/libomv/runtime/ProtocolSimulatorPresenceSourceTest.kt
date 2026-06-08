package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.LibomvPacketTestBytes
import org.hostess.protocol.libomv.transport.LibomvZerocodeCodec
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorEndpoint
import org.hostess.protocol.libomv.transport.SimulatorInboundPacket
import org.hostess.protocol.libomv.transport.SimulatorPacketExchange
import org.hostess.protocol.libomv.transport.SimulatorPacketSequence

class ProtocolSimulatorPresenceSourceTest {
    @Test
    fun `maps circuit presence success to passed proof fields`() {
        val source = ProtocolSimulatorPresenceSource(
            ProtocolSimulatorCircuitClient(
                packetExchange = RecordingPacketExchange(
                    inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
                ),
                sequence = SimulatorPacketSequence(0),
            ),
        )

        val proof = assertIs<SimulatorPresenceProofResult.Success>(
            source.simulatorPresence(identity()),
        ).proof

        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.regionHandshakeStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.regionHandshakeReplyStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.agentMovementStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, proof.agentUpdateStatus)
    }

    @Test
    fun `maps movement timeout to proof gap without claiming agent update`() {
        val source = ProtocolSimulatorPresenceSource(
            ProtocolSimulatorCircuitClient(
                packetExchange = RecordingPacketExchange(
                    inboundPayloads = mutableListOf(regionHandshake()),
                ),
                sequence = SimulatorPacketSequence(0),
            ),
        )

        val failure = assertIs<SimulatorPresenceProofResult.Failure>(
            source.simulatorPresence(identity()),
        )

        assertEquals(SimulatorPresenceProofStatus.PROOF_GAP, failure.proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, failure.proof.regionHandshakeStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, failure.proof.regionHandshakeReplyStatus)
        assertEquals(SimulatorPresenceProofStatus.PROOF_GAP, failure.proof.agentMovementStatus)
        assertEquals(SimulatorPresenceProofStatus.NOT_RUN, failure.proof.agentUpdateStatus)
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

    private fun regionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 101, packetId = 148, flags = 0xC0),
        )

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private class RecordingPacketExchange(
        private val inboundPayloads: MutableList<ByteArray> = mutableListOf(),
    ) : SimulatorPacketExchange {
        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) = Unit

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
        const val CIRCUIT_CODE = 0x01020304L
    }
}
