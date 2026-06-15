package org.gem.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecondLifeLoginSecretPolicyTest {
    @Test
    fun `normalizes raw second life password paste whitespace`() {
        assertEquals(
            "SecretPass16Byte",
            SecondLifeLoginSecretPolicy.normalizeForStorage("  SecretPass16Byte\n"),
        )
    }

    @Test
    fun `normalizes hashed second life password to lower case`() {
        assertEquals(
            "\$1\$2686b1527287c27ff20744a4c4f07870",
            SecondLifeLoginSecretPolicy.normalizeForStorage("\$1\$2686B1527287C27FF20744A4C4F07870"),
        )
    }

    @Test
    fun `rejects blank overlong non ascii and malformed hashes`() {
        assertNull(SecondLifeLoginSecretPolicy.normalizeForStorage(""))
        assertNull(SecondLifeLoginSecretPolicy.normalizeForStorage("   "))
        assertNull(SecondLifeLoginSecretPolicy.normalizeForStorage("12345678901234567"))
        assertNull(SecondLifeLoginSecretPolicy.normalizeForStorage("secrét"))
        assertNull(SecondLifeLoginSecretPolicy.normalizeForStorage("\$1\$not-md5"))
    }
}
