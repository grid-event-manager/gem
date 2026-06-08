package org.hostess.protocol.libomv.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibomvPacketCodecTest {
    @Test
    fun `parses fixed packet ack sequence numbers`() {
        val ack = byteArrayOf(
            0,
            0,
            0,
            0,
            77,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            2,
        ) + u32(6) + u32(123456789)

        assertEquals(listOf(6L, 123456789L), LibomvPacketCodec.packetAckSequences(ack))
    }

    @Test
    fun `parses appended packet ack sequence numbers`() {
        val packetWithAppendedAcks = LibomvPacketTestBytes.highHeader(
            sequence = 77,
            packetId = 1,
            flags = 0x10,
        ) + byteArrayOf(7) + u32BigEndian(6) + u32BigEndian(123456789) + byteArrayOf(2)

        assertEquals(listOf(6L, 123456789L), LibomvPacketCodec.packetAckSequences(packetWithAppendedAcks))
    }

    @Test
    fun `rejects malformed packet ack`() {
        val ack = byteArrayOf(
            0,
            0,
            0,
            0,
            77,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            2,
        ) + u32(6)

        assertNull(LibomvPacketCodec.packetAckSequences(ack))
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun u32BigEndian(value: Long): ByteArray = byteArrayOf(
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )
}
