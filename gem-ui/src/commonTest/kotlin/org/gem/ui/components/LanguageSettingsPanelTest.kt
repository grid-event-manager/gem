package org.gem.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.gem.core.language.LanguagePreference
import org.gem.ui.state.LanguageOption
import org.gem.ui.state.LanguageUiState
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey

class LanguageSettingsPanelTest {
    private val text = EnglishGemTextCatalogue

    @Test
    fun languagePanelOwnsOnlyLanguageCard() {
        assertEquals(listOf(GemTextKey.Language), LanguageSettingsPanelInteraction.contentOrder)
    }

    @Test
    fun dropdownOptionsUseCentralCopyAndLocaleNativeNames() {
        val options = LanguageSettingsPanelInteraction.options(state(), text)

        assertEquals(text.text(GemTextKey.ChooseLanguage), options[0].label)
        assertNull(options[0].value)
        assertFalse(options[0].enabled)
        assertEquals(text.text(GemTextKey.SystemLanguage), options[1].label)
        assertEquals(LanguageOption.System, options[1].value)
        assertEquals("English", options[2].label)
        assertEquals(LanguageOption.Locale("en-GB", "English"), options[2].value)
    }

    @Test
    fun selectedLabelUsesPreferenceAndUnavailableLocaleFallsBackToPlaceholder() {
        assertEquals(
            text.text(GemTextKey.SystemLanguage),
            LanguageSettingsPanelInteraction.selectedLabel(state(), text),
        )
        assertEquals(
            "English",
            LanguageSettingsPanelInteraction.selectedLabel(
                state().copy(preference = LanguagePreference.Locale("en-GB")),
                text,
            ),
        )
        assertNull(
            LanguageSettingsPanelInteraction.selectedLabel(
                state().copy(preference = LanguagePreference.Locale("zz-ZZ")),
                text,
            ),
        )
    }

    @Test
    fun warningKeyComesFromStateOnly() {
        assertNull(LanguageSettingsPanelInteraction.warningKey(state()))
        assertEquals(
            GemTextKey.LanguagePreferenceUnavailable,
            LanguageSettingsPanelInteraction.warningKey(
                state().copy(warningKey = GemTextKey.LanguagePreferenceUnavailable),
            ),
        )
    }

    private fun state(): LanguageUiState =
        LanguageUiState(
            preference = LanguagePreference.System,
            requestedLocaleTag = "en-GB",
            resolvedLocaleTag = "en-GB",
            options = listOf(
                LanguageOption.Placeholder,
                LanguageOption.System,
                LanguageOption.Locale("en-GB", "English"),
            ),
        )
}
