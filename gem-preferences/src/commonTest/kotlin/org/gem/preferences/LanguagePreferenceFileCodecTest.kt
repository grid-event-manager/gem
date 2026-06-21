package org.gem.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult

class LanguagePreferenceFileCodecTest {
    private val codec = LanguagePreferenceFileCodec()

    @Test
    fun encodesSystemAndLocalePreferencesAsSingleSupportedKey() {
        assertEquals("languagePreference=SYSTEM\n", codec.encode(LanguagePreference.System))
        assertEquals("languagePreference=LOCALE:fr-FR\n", codec.encode(LanguagePreference.Locale("fr-FR")))
        assertEquals("languagePreference=LOCALE:EN_us\n", codec.encode(LanguagePreference.Locale("EN_us")))
    }

    @Test
    fun decodesValidPreferenceValuesWithSurroundingWhitespaceIgnored() {
        val system = assertIs<LanguagePreferenceLoadResult.Loaded>(
            codec.decode("\n languagePreference=SYSTEM \n"),
        )
        val locale = assertIs<LanguagePreferenceLoadResult.Loaded>(
            codec.decode("\n languagePreference=LOCALE:uk-UA \n"),
        )

        assertEquals(LanguagePreference.System, system.preference)
        assertEquals(LanguagePreference.Locale("uk-UA"), locale.preference)
    }

    @Test
    fun rejectsMissingBlankDuplicateUnknownExtraAndBlankLocaleRecords() {
        assertInvalid(null, codec.decode(""))
        assertInvalid("", codec.decode("languagePreference="))
        assertInvalid("AUTO", codec.decode("languagePreference=AUTO"))
        assertInvalid("LOCALE:", codec.decode("languagePreference=LOCALE:"))
        assertInvalid("languagePreference=SYSTEM\nother=LOCALE:fr-FR", codec.decode("languagePreference=SYSTEM\nother=LOCALE:fr-FR"))
        assertInvalid("other=SYSTEM", codec.decode("other=SYSTEM"))
        assertInvalid(
            "languagePreference=SYSTEM\nlanguagePreference=LOCALE:fr-FR",
            codec.decode("languagePreference=SYSTEM\nlanguagePreference=LOCALE:fr-FR"),
        )
        assertInvalid("languagePreference SYSTEM", codec.decode("languagePreference SYSTEM"))
    }

    private fun assertInvalid(
        expectedRawValue: String?,
        result: LanguagePreferenceLoadResult,
    ) {
        val invalid = assertIs<LanguagePreferenceLoadResult.InvalidValue>(result)
        assertEquals(expectedRawValue, invalid.rawValue)
    }
}
