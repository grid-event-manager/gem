package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.mapping.LibomvNoticePosition
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SimulatorSessionProtocolTest {
    @Test
    fun `builds presence and heartbeat packets with fixed method surface`() {
        val protocol = SimulatorSessionProtocol()

        assertLowPacket(protocol.useCircuitCode(circuit()).payload, sequence = 1, packetId = 3)
        assertLowPacket(protocol.regionHandshakeReply(circuit()).payload, sequence = 2, packetId = 149, flags = 0xC0)
        assertLowPacket(protocol.completeAgentMovement(circuit()).payload, sequence = 3, packetId = 249)
        assertHighPacket(protocol.initialAgentUpdate(circuit()).payload, sequence = 4, packetId = 4, flags = 0xC0)
        assertHighPacket(protocol.heartbeat(circuit()).payload, sequence = 5, packetId = 4, flags = 0x80)
        assertEquals(SimulatorSessionHealthStatus.PRESENT, protocol.health().status)
    }

    @Test
    fun `acks inbound reliable packets even when packet type is unknown`() {
        val protocol = SimulatorSessionProtocol()
        val actions = protocol.onInbound(circuit(), layerData(sequence = 201, flags = 0x40))

        val ack = assertIs<AckReliable>(actions.single())
        assertEquals(201L, ack.sequenceNumber)
        assertPacketAck(ack.packet, ackedSequence = 201)
    }

    @Test
    fun `records outgoing reliable ack and stops resend tracking`() {
        val protocol = SimulatorSessionProtocol()
        val notice = protocol.notice(circuit(), noticePacket())

        val actions = protocol.onInbound(circuit(), simulatorPacketAck(notice.reliableSequenceNumber ?: error("missing sequence")))

        val ack = assertIs<RecordOutgoingAck>(actions.single())
        assertEquals(notice.reliableSequenceNumber, ack.sequenceNumber)
        assertIs<AwaitAck>(protocol.onReliableSendTimeout(ack.sequenceNumber))
    }

    @Test
    fun `answers valid ping and flags malformed ping`() {
        val protocol = SimulatorSessionProtocol()

        val reply = assertIs<ReplyToPing>(protocol.onInbound(circuit(), startPingCheck(7)).single())
        assertEquals(7, reply.pingId)
        assertHighPacket(reply.packet, sequence = 1, packetId = 2)

        val failed = assertIs<MarkFailed>(protocol.onInbound(circuit(), malformedStartPingCheck()).single())
        assertEquals("simulator ping malformed", failed.redactedMessage)
    }

    @Test
    fun `matches only requested archive replies`() {
        val protocol = SimulatorSessionProtocol()
        protocol.noticeArchiveRequest(circuit(), GROUP_ID)

        val wrongGroupActions = protocol.onInbound(circuit(), groupNoticesListReply(groupId = OTHER_GROUP_ID))
        assertEquals(1, wrongGroupActions.filterIsInstance<AckReliable>().size)
        assertTrue(wrongGroupActions.filterIsInstance<MatchedArchiveReply>().isEmpty())
        val matched = assertIs<MatchedArchiveReply>(
            protocol.onInbound(circuit(), groupNoticesListReply(groupId = GROUP_ID))
                .filterIsInstance<MatchedArchiveReply>()
                .single(),
        )
        assertEquals(GROUP_ID, matched.groupId)
        assertEquals("Tonight", matched.entries.single().subject)
    }

    @Test
    fun `matches logout reply and marks session closed`() {
        val protocol = SimulatorSessionProtocol()
        protocol.logoutRequest(circuit())

        val actions = protocol.onInbound(circuit(), logoutReply())
        assertEquals(1, actions.filterIsInstance<AckReliable>().size)
        assertIs<MatchedLogoutReply>(actions.filterIsInstance<MatchedLogoutReply>().single())
        assertEquals(SimulatorSessionHealthStatus.CLOSED, protocol.health().status)
    }

    @Test
    fun `retries reliable sends then marks health failed on timeout`() {
        val protocol = SimulatorSessionProtocol()
        val notice = protocol.notice(circuit(), noticePacket())
        val sequenceNumber = notice.reliableSequenceNumber ?: error("missing sequence")

        val first = assertIs<Resend>(protocol.onReliableSendTimeout(sequenceNumber))
        val second = assertIs<Resend>(protocol.onReliableSendTimeout(sequenceNumber))
        val timeout = assertIs<TimedOut>(protocol.onReliableSendTimeout(sequenceNumber))

        assertEquals(sequenceNumber, first.packet.reliableSequenceNumber)
        assertEquals(sequenceNumber, second.packet.reliableSequenceNumber)
        assertEquals("reliable simulator send ack timeout after 3 attempts", timeout.redactedMessage)
        assertEquals(SimulatorSessionHealthStatus.FAILED, protocol.health().status)
    }

    @Test
    fun `observes simulator notice and alert traffic with redacted summaries`() {
        val protocol = SimulatorSessionProtocol()

        val notice = assertIs<ObserveNoticeTraffic>(
            protocol.onInbound(circuit(), improvedInstantMessage("Accepted for $GROUP_ID"))
                .filterIsInstance<ObserveNoticeTraffic>()
                .single(),
        )
        val alert = assertIs<ObserveNoticeTraffic>(
            protocol.onInbound(circuit(), alertMessage("No permission for $GROUP_ID"))
                .filterIsInstance<ObserveNoticeTraffic>()
                .single(),
        )

        assertTrue(notice.redactedSummary.contains("messageLength="))
        assertTrue(alert.redactedSummary.contains("[redacted-id]"))
    }

    private fun assertLowPacket(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        flags: Int = 0,
    ) {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        val expected = LibomvPacketTestBytes.lowHeader(sequence, packetId, flags)
        assertContentEquals(expected, decoded.copyOfRange(0, expected.size))
    }

    private fun assertHighPacket(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        flags: Int = 0,
    ) {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        val expected = LibomvPacketTestBytes.highHeader(sequence, packetId, flags)
        assertContentEquals(expected, decoded.copyOfRange(0, expected.size))
    }

    private fun assertPacketAck(payload: ByteArray, ackedSequence: Long) {
        val expected = byteArrayOf(
            0,
            0,
            0,
            0,
            0,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            1,
        ) + u32(ackedSequence)
        assertContentEquals(expected, payload)
    }

    private fun circuit(): SimulatorCircuit = SimulatorCircuit(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private fun layerData(sequence: Int, flags: Int = 0): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = sequence, packetId = 11, flags = flags) + byteArrayOf(0)

    private fun startPingCheck(pingId: Int): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = 103, packetId = 1) +
            byteArrayOf(pingId.toByte()) +
            u32(0L)

    private fun malformedStartPingCheck(): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = 103, packetId = 1)

    private fun simulatorPacketAck(ackedSequence: Long): ByteArray =
        byteArrayOf(
            0,
            0,
            0,
            0,
            105,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            1,
        ) + u32(ackedSequence)

    private fun groupNoticesListReply(groupId: String): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 104, packetId = 59, flags = 0x40) +
            LibomvPacketTestBytes.uuid(AGENT_ID) +
            LibomvPacketTestBytes.uuid(groupId) +
            byteArrayOf(1) +
            LibomvPacketTestBytes.uuid("99999999-9999-9999-9999-999999999999") +
            u32(1_717_000_000L) +
            variable2("venue-proof") +
            variable2("Tonight") +
            byteArrayOf(1, 3)

    private fun logoutReply(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 13, packetId = 253, flags = 0xC0) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                byteArrayOf(0),
        )

    private fun improvedInstantMessage(message: String): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 106, packetId = 254, flags = 0x40) +
            LibomvPacketTestBytes.uuid(AGENT_ID) +
            LibomvPacketTestBytes.uuid(SESSION_ID) +
            byteArrayOf(0) +
            LibomvPacketTestBytes.uuid(GROUP_ID) +
            u32(0L) +
            LibomvPacketTestBytes.uuid(ZERO_ID) +
            ByteArray(12) +
            byteArrayOf(0, 32) +
            LibomvPacketTestBytes.uuid("44444444-4444-4444-4444-444444444444") +
            u32(0L) +
            variable1("Second Life") +
            variable2(message) +
            variable2("")

    private fun alertMessage(message: String): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 107, packetId = 134, flags = 0x40) +
            variable1(message) +
            byteArrayOf(0)

    private fun noticePacket(): LibomvNoticePacket = LibomvNoticePacket(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        fromGroup = false,
        targetGroupId = GROUP_ID,
        fromAgentName = "venue-proof",
        message = "Gig tonight|Doors at 8",
        dialog = 32,
        offline = 0,
        instantMessageId = "44444444-4444-4444-4444-444444444444",
        parentEstateId = 0,
        timestamp = 0,
        regionId = ZERO_ID,
        position = LibomvNoticePosition.ZERO,
        attachment = null,
        binaryBucket = ByteArray(0),
    )

    private fun variable2(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(
            (bytes.size and 0xFF).toByte(),
            ((bytes.size ushr 8) and 0xFF).toByte(),
        ) + bytes
    }

    private fun variable1(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(bytes.size.toByte()) + bytes
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val OTHER_GROUP_ID = "55555555-5555-5555-5555-555555555555"
        const val ZERO_ID = "00000000-0000-0000-0000-000000000000"
    }
}
