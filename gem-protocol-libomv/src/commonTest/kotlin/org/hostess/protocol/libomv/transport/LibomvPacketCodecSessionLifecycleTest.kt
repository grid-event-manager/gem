package org.hostess.protocol.libomv.transport

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibomvPacketCodecSessionLifecycleTest {
    @Test
    fun `agent heartbeat can be sent as unreliable zerocoded update`() {
        val payload = LibomvPacketCodec.agentUpdate(circuit(), sequence = 7, reliable = false)

        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        assertContentEquals(
            LibomvPacketTestBytes.highHeader(sequence = 7, packetId = 4, flags = 0x80) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID),
            decoded.copyOfRange(0, 39),
        )
    }

    @Test
    fun `logout request is reliable low packet with agent and session identity`() {
        val payload = LibomvPacketCodec.logoutRequest(circuit(), sequence = 9)

        assertLowPacket(
            payload = payload,
            sequence = 9,
            packetId = 252,
            flags = 0x40,
            body = LibomvPacketTestBytes.uuid(AGENT_ID) + LibomvPacketTestBytes.uuid(SESSION_ID),
        )
    }

    @Test
    fun `close circuit uses fixed packet shape`() {
        val payload = LibomvPacketCodec.closeCircuit(sequence = 11)

        assertContentEquals(
            byteArrayOf(
                0,
                0,
                0,
                0,
                11,
                0,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFD.toByte(),
            ),
            payload,
        )
    }

    @Test
    fun `logout reply is classified and matched by active identity only`() {
        val matching = logoutReply(agentId = AGENT_ID, sessionId = SESSION_ID)
        val wrongAgent = logoutReply(agentId = OTHER_ID, sessionId = SESSION_ID)
        val malformed = LibomvPacketTestBytes.lowHeader(sequence = 14, packetId = 253, flags = 0xC0) +
            LibomvPacketTestBytes.uuid(AGENT_ID)

        assertEquals(SimulatorPacketType.LOGOUT_REPLY, LibomvPacketCodec.packetType(matching))
        assertTrue(LibomvPacketCodec.logoutReplyMatches(matching, circuit()))
        assertFalse(LibomvPacketCodec.logoutReplyMatches(wrongAgent, circuit()))
        assertFalse(LibomvPacketCodec.logoutReplyMatches(malformed, circuit()))
    }

    private fun assertLowPacket(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        flags: Int,
        body: ByteArray,
    ) {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        val expected = LibomvPacketTestBytes.lowHeader(sequence, packetId, flags) + body
        assertContentEquals(expected, decoded)
    }

    private fun logoutReply(agentId: String, sessionId: String): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 13, packetId = 253, flags = 0xC0) +
                LibomvPacketTestBytes.uuid(agentId) +
                LibomvPacketTestBytes.uuid(sessionId) +
                byteArrayOf(1) +
                LibomvPacketTestBytes.uuid("99999999-9999-9999-9999-999999999999"),
        )

    private fun circuit(): SimulatorCircuit = SimulatorCircuit(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val OTHER_ID = "33333333-3333-3333-3333-333333333333"
    }
}
