package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.GroupMembership
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.LibomvPacketTestBytes
import org.hostess.protocol.libomv.transport.LibomvZerocodeCodec
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorEndpoint
import org.hostess.protocol.libomv.transport.SimulatorInboundPacket
import org.hostess.protocol.libomv.transport.SimulatorPacketExchange
import org.hostess.protocol.libomv.transport.SimulatorPacketSequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ProtocolGroupNoticeArchiveSourceTest {
    @Test
    fun `maps archive entries without exposing notice ids`() {
        val exchange = RecordingPacketExchange(
            mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                groupNoticesListReply(),
            ),
        )
        val source = ProtocolGroupNoticeArchiveSource(
            ProtocolSimulatorCircuitClient(exchange, SimulatorPacketSequence(0)),
        )

        val result = assertIs<GroupNoticeArchiveResult.Success>(
            source.noticeArchive(identity(), group()),
        )

        val entry = result.entries.single()
        assertEquals("Tonight", entry.subject)
        assertEquals("venue-proof", entry.fromName)
        assertEquals(1_717_000_000L, entry.timestamp)
        assertEquals(true, entry.hasAttachment)
        assertEquals(3, entry.assetType)
        assertFalse(entry.toString().contains("99999999"))
    }

    @Test
    fun `maps archive proof gap to redacted failure`() {
        val exchange = RecordingPacketExchange(
            mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val source = ProtocolGroupNoticeArchiveSource(
            ProtocolSimulatorCircuitClient(exchange, SimulatorPacketSequence(0)),
        )

        val result = assertIs<GroupNoticeArchiveResult.Failure>(
            source.noticeArchive(identity(), group()),
        )

        assertEquals("notice archive proof_gap", result.failure.redactedMessage)
    }

    private class RecordingPacketExchange(
        private val inboundPayloads: MutableList<ByteArray>,
    ) : SimulatorPacketExchange {
        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) = Unit

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            if (inboundPayloads.isEmpty()) {
                null
            } else {
                SimulatorInboundPacket(endpoint, inboundPayloads.removeAt(0))
            }
    }

    private fun regionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 101, packetId = 148, flags = 0xC0),
        )

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private fun groupNoticesListReply(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 103, packetId = 59, flags = 0) +
            LibomvPacketTestBytes.uuid(AGENT_ID) +
            LibomvPacketTestBytes.uuid(GROUP_ID) +
            byteArrayOf(1) +
            LibomvPacketTestBytes.uuid(NOTICE_ID) +
            u32(1_717_000_000L) +
            variable2("venue-proof") +
            variable2("Tonight") +
            byteArrayOf(1, 3)

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private fun group(): GroupMembership =
        GroupMembership.fromValues(GROUP_ID, "Venue Hosts", true, true)

    private fun variable2(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(
            (bytes.size and 0xFF).toByte(),
            ((bytes.size ushr 8) and 0xFF).toByte(),
        ) + bytes
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
        const val NOTICE_ID = "99999999-9999-9999-9999-999999999999"
    }
}
