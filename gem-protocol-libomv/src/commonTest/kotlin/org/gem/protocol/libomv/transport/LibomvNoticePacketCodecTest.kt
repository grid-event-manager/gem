package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.mapping.LibomvNoticePosition
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LibomvNoticePacketCodecTest {
    @Test
    fun `encodes improved instant message in source field order`() {
        val packet = noticePacket(
            binaryBucket = byteArrayOf(1, 0, 2, 3),
        )

        val encoded = LibomvNoticePacketCodec.improvedInstantMessage(packet, SEQUENCE)
        val decoded = LibomvPacketTestBytes.zeroDecode(encoded)

        assertEquals(0xC0.toByte(), encoded[0])
        assertTrue(encoded.containsZeroRun())
        assertContentEquals(
            LibomvPacketTestBytes.lowHeader(SEQUENCE) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                byteArrayOf(0) +
                LibomvPacketTestBytes.uuid(TARGET_GROUP_ID) +
                u32(PARENT_ESTATE_ID) +
                LibomvPacketTestBytes.uuid(REGION_ID) +
                f32(1.0f) +
                f32(-2.5f) +
                f32(3.25f) +
                byteArrayOf(OFFLINE.toByte()) +
                byteArrayOf(DIALOG.toByte()) +
                LibomvPacketTestBytes.uuid(INSTANT_MESSAGE_ID) +
                u32(TIMESTAMP) +
                variable1("Gem Viewer") +
                variable2("Set|Doors open") +
                variable2(byteArrayOf(1, 0, 2, 3)),
            decoded,
        )
    }

    @Test
    fun `encodes empty binary bucket as zero variable length`() {
        val packet = noticePacket(binaryBucket = ByteArray(0))

        val decoded = LibomvPacketTestBytes.zeroDecode(
            LibomvNoticePacketCodec.improvedInstantMessage(packet, SEQUENCE),
        )

        assertContentEquals(
            LibomvPacketTestBytes.lowHeader(SEQUENCE) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                byteArrayOf(0) +
                LibomvPacketTestBytes.uuid(TARGET_GROUP_ID) +
                u32(PARENT_ESTATE_ID) +
                LibomvPacketTestBytes.uuid(REGION_ID) +
                f32(1.0f) +
                f32(-2.5f) +
                f32(3.25f) +
                byteArrayOf(OFFLINE.toByte()) +
                byteArrayOf(DIALOG.toByte()) +
                LibomvPacketTestBytes.uuid(INSTANT_MESSAGE_ID) +
                u32(TIMESTAMP) +
                variable1("Gem Viewer") +
                variable2("Set|Doors open") +
                byteArrayOf(0, 0),
            decoded,
        )
    }

    @Test
    fun `rejects malformed uuids before packet return`() {
        assertFailsWith<IllegalArgumentException> {
            LibomvNoticePacketCodec.improvedInstantMessage(
                noticePacket(agentId = "not-a-uuid"),
                SEQUENCE,
            )
        }
    }

    @Test
    fun `rejects variable data exceeding template lengths`() {
        assertFailsWith<IllegalArgumentException> {
            LibomvNoticePacketCodec.improvedInstantMessage(
                noticePacket(fromAgentName = "a".repeat(255)),
                SEQUENCE,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LibomvNoticePacketCodec.improvedInstantMessage(
                noticePacket(message = "b".repeat(65_535)),
                SEQUENCE,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LibomvNoticePacketCodec.improvedInstantMessage(
                noticePacket(binaryBucket = ByteArray(65_536)),
                SEQUENCE,
            )
        }
    }

    private fun noticePacket(
        agentId: String = AGENT_ID,
        fromAgentName: String = "Gem Viewer",
        message: String = "Set|Doors open",
        binaryBucket: ByteArray = ByteArray(0),
    ): LibomvNoticePacket = LibomvNoticePacket(
        agentId = agentId,
        sessionId = SESSION_ID,
        fromGroup = false,
        targetGroupId = TARGET_GROUP_ID,
        fromAgentName = fromAgentName,
        message = message,
        dialog = DIALOG,
        offline = OFFLINE,
        instantMessageId = INSTANT_MESSAGE_ID,
        parentEstateId = PARENT_ESTATE_ID,
        timestamp = TIMESTAMP,
        regionId = REGION_ID,
        position = LibomvNoticePosition(1.0, -2.5, 3.25),
        attachment = null,
        binaryBucket = binaryBucket,
    )

    private fun variable1(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(bytes.size.toByte()) + bytes
    }

    private fun variable2(value: String): ByteArray =
        variable2(value.encodeToByteArray() + 0.toByte())

    private fun variable2(value: ByteArray): ByteArray =
        byteArrayOf(
            (value.size and 0xFF).toByte(),
            ((value.size ushr 8) and 0xFF).toByte(),
        ) + value

    private fun u32(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun f32(value: Float): ByteArray {
        val bits = value.toBits()
        return byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits ushr 8) and 0xFF).toByte(),
            ((bits ushr 16) and 0xFF).toByte(),
            ((bits ushr 24) and 0xFF).toByte(),
        )
    }

    private fun ByteArray.containsZeroRun(): Boolean {
        val headerLength = 6 + (this[5].toInt() and 0xFF)
        for (index in headerLength until lastIndex) {
            if (this[index] == 0.toByte() && (this[index + 1].toInt() and 0xFF) > 0) {
                return true
            }
        }
        return false
    }

    private companion object {
        const val AGENT_ID = "00112233-4455-6677-8899-aabbccddeeff"
        const val SESSION_ID = "10213243-5465-7687-98a9-bacbdcedfe0f"
        const val TARGET_GROUP_ID = "fedcba98-7654-3210-0011-223344556677"
        const val INSTANT_MESSAGE_ID = "01020304-0506-0708-090a-0b0c0d0e0f10"
        const val REGION_ID = "11111111-2222-3333-4444-555555555555"
        const val SEQUENCE = 0x01020304
        const val PARENT_ESTATE_ID = 7
        const val TIMESTAMP = 1000
        const val OFFLINE = 0
        const val DIALOG = 32
    }
}
