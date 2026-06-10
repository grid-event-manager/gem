package org.hostess.preferences

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ThemePreferenceFileCodecTest {
    private val codec = ThemePreferenceFileCodec()

    @Test
    fun `encodes each preference as the single supported key`() {
        assertEquals("themePreference=SYSTEM\n", codec.encode(ThemePreference.SYSTEM))
        assertEquals("themePreference=LIGHT\n", codec.encode(ThemePreference.LIGHT))
        assertEquals("themePreference=DARK\n", codec.encode(ThemePreference.DARK))
    }

    @Test
    fun `decodes valid preference values with surrounding whitespace ignored`() {
        val decoded = assertIs<ThemePreferenceLoadResult.Loaded>(
            codec.decode("\n themePreference=LIGHT \n"),
        )

        assertEquals(ThemePreference.LIGHT, decoded.preference)
    }

    @Test
    fun `rejects missing blank duplicate unknown and extra records`() {
        assertInvalid(null, codec.decode(""))
        assertInvalid("", codec.decode("themePreference="))
        assertInvalid("BLUE", codec.decode("themePreference=BLUE"))
        assertInvalid("themePreference=LIGHT\nother=DARK", codec.decode("themePreference=LIGHT\nother=DARK"))
        assertInvalid("other=LIGHT", codec.decode("other=LIGHT"))
        assertInvalid("themePreference=LIGHT\nthemePreference=DARK", codec.decode("themePreference=LIGHT\nthemePreference=DARK"))
        assertInvalid("themePreference LIGHT", codec.decode("themePreference LIGHT"))
    }

    private fun assertInvalid(
        expectedRawValue: String?,
        result: ThemePreferenceLoadResult,
    ) {
        val invalid = assertIs<ThemePreferenceLoadResult.InvalidValue>(result)
        assertEquals(expectedRawValue, invalid.rawValue)
    }
}
