package org.hostess.protocol.libomv.mapping

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibomvUuidCodecTest {
    @Test
    fun `canonicalizes UUID text and rejects malformed text`() {
        assertEquals(
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            LibomvUuidCodec.canonicalOrNull("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"),
        )
        assertNull(LibomvUuidCodec.canonicalOrNull("not-a-uuid"))
        assertNull(LibomvUuidCodec.canonicalOrNull("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee"))
    }

    @Test
    fun `xors UUIDs and emits packet bytes`() {
        assertEquals(
            "22222222-2222-2222-2222-222222222222",
            LibomvUuidCodec.xor(
                "33333333-3333-3333-3333-333333333333",
                "11111111-1111-1111-1111-111111111111",
            ),
        )
        assertContentEquals(
            byteArrayOf(
                0x01,
                0x23,
                0x45,
                0x67,
                0x89.toByte(),
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x01,
                0x23,
                0x45,
                0x67,
                0x89.toByte(),
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
            ),
            LibomvUuidCodec.packetBytes("01234567-89ab-cdef-0123-456789abcdef"),
        )
    }
}
