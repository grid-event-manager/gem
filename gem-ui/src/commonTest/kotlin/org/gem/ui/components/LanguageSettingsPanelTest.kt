package org.gem.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.gem.core.language.LanguagePreference
import org.gem.ui.state.LanguageOption
import org.gem.ui.state.LanguageUiState
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.ExpectedGemLocalizationLocales
import org.gem.ui.text.GemTextKey

class LanguageSettingsPanelTest {
    private val text = EnglishGemTextCatalogue

    @Test
    fun languagePanelOwnsSingleDropdownLikeThemes() {
        assertEquals(listOf("language-dropdown"), LanguageSettingsPanelInteraction.contentOrder)
    }

    @Test
    fun dropdownOptionsUseCentralCopyAndGeneratedLocaleNativeNames() {
        val options = LanguageSettingsPanelInteraction.options(state(), text)

        assertEquals(2 + ExpectedGemLocalizationLocales.localeTags.size, options.size)
        assertEquals(text.text(GemTextKey.ChooseLanguage), options[0].label)
        assertNull(options[0].value)
        assertFalse(options[0].enabled)
        assertEquals(text.text(GemTextKey.SystemLanguage), options[1].label)
        assertEquals(LanguageOption.System, options[1].value)
        assertEquals(
            ExpectedGemLocalizationLocales.localeTags.map { localeTag ->
                ExpectedGemLocalizationLocales.nativeNamesByLocaleTag.getValue(localeTag)
            },
            options.drop(2).map { it.label },
        )
        assertEquals(
            expectedLocaleOptions(),
            options.drop(2).map { it.value },
        )
    }

    @Test
    fun selectedLabelUsesPreferenceAndUnavailableLocaleFallsBackToPlaceholder() {
        assertEquals(
            text.text(GemTextKey.SystemLanguage),
            LanguageSettingsPanelInteraction.selectedLabel(state(), text),
        )
        assertEquals(
            "Français",
            LanguageSettingsPanelInteraction.selectedLabel(
                state().copy(preference = LanguagePreference.Locale("fr-FR")),
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
            options = listOf(LanguageOption.Placeholder, LanguageOption.System) + expectedLocaleOptions(),
        )

    private fun expectedLocaleOptions(): List<LanguageOption.Locale> =
        ExpectedGemLocalizationLocales.localeTags.map { localeTag ->
            LanguageOption.Locale(
                localeTag = localeTag,
                nativeName = ExpectedGemLocalizationLocales.nativeNamesByLocaleTag.getValue(localeTag),
            )
        }
}
