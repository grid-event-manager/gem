package org.gem.core.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AppearanceColorTest {
    @Test
    fun `colour parser accepts compact and full hex inputs`() {
        assertEquals("#AABBCC", parsed("abc").value)
        assertEquals("#AABBCC", parsed("#abc").value)
        assertEquals("#A1B2C3", parsed("a1b2c3").value)
        assertEquals("#A1B2C3", parsed("#a1b2c3").value)
    }

    @Test
    fun `colour parser rejects malformed and out of range inputs`() {
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from(""))
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from("#12"))
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from("#1234"))
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from("#12345"))
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from("#1234567"))
        assertIs<AppearanceColorParseResult.Invalid>(AppearanceColor.from("#ggg"))
    }

    @Test
    fun `strict colour creation rejects invalid values`() {
        assertFailsWith<IllegalArgumentException> {
            AppearanceColor.require("not-a-colour")
        }
    }

    private fun parsed(input: String): AppearanceColor =
        assertIs<AppearanceColorParseResult.Valid>(AppearanceColor.from(input)).color
}
