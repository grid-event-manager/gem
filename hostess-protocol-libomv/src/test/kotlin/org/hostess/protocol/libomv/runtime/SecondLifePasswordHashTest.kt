package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecondLifePasswordHashTest {
    @Test
    fun `raw ascii secret hashes to second life md5 wire form`() {
        val hash = SecondLifePasswordHash.fromSharedSecret("secret12")

        assertEquals("\$1\$8b2fee48cd255fddee9a662b55da4fd4", hash?.wireValue)
    }

    @Test
    fun `valid hash is preserved with lower case hex`() {
        val hash = SecondLifePasswordHash.fromSharedSecret("\$1\$2686B1527287C27FF20744A4C4F07870")

        assertEquals("\$1\$2686b1527287c27ff20744a4c4f07870", hash?.wireValue)
    }

    @Test
    fun `valid hash does not call digest port`() {
        val hash = SecondLifePasswordHash.fromSharedSecret(
            "\$1\$2686B1527287C27FF20744A4C4F07870",
            Md5DigestPort { error("pre-hashed secret must not be digested") },
        )

        assertEquals("\$1\$2686b1527287c27ff20744a4c4f07870", hash?.wireValue)
    }

    @Test
    fun `raw ascii secret uses supplied digest port`() {
        val hash = SecondLifePasswordHash.fromSharedSecret(
            "secret12",
            Md5DigestPort { chunks ->
                assertEquals(1, chunks.size)
                assertContentEquals("secret12".encodeToByteArray(), chunks.single())
                "00000000000000000000000000000001"
            },
        )

        assertEquals("\$1\$00000000000000000000000000000001", hash?.wireValue)
    }

    @Test
    fun `invalid secrets fail closed`() {
        assertNull(SecondLifePasswordHash.fromSharedSecret(""))
        assertNull(SecondLifePasswordHash.fromSharedSecret("   "))
        assertNull(SecondLifePasswordHash.fromSharedSecret("\$1\$not-md5"))
        assertNull(SecondLifePasswordHash.fromSharedSecret("12345678901234567"))
        assertNull(SecondLifePasswordHash.fromSharedSecret("secrét"))
    }
}
