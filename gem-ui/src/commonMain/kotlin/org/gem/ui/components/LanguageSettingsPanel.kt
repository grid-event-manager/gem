package org.gem.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import org.gem.core.language.LanguagePreference
import org.gem.ui.design.GemTheme
import org.gem.ui.state.LanguageOption
import org.gem.ui.state.LanguageUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

data class LanguagePanelCallbacks(
    val onOptionSelected: (LanguageOption) -> Unit,
)

@Composable
fun LanguageSettingsPanel(
    state: LanguageUiState,
    textCatalogue: GemTextCatalogue,
    callbacks: LanguagePanelCallbacks,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GemPanel(modifier = modifier.testTag(GemTestTags.LanguagePanel)) {
        GemUnlabelledDropdownField(
            selectedLabel = LanguageSettingsPanelInteraction.selectedLabel(state, textCatalogue),
            placeholderLabel = textCatalogue.text(GemTextKey.ChooseLanguage),
            options = LanguageSettingsPanelInteraction.options(state, textCatalogue),
            onSelected = { selected -> selected?.let(callbacks.onOptionSelected) },
            enabled = enabled,
            fieldModifier = Modifier.testTag(GemTestTags.LanguageDropdown),
            textAlign = TextAlign.Center,
        )
        LanguageSettingsPanelInteraction.warningKey(state)?.let { warningKey ->
            Text(
                text = textCatalogue.text(warningKey),
                color = GemTheme.colors.danger,
                style = GemTheme.typeScale.smallLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GemTestTags.LanguageWarning),
            )
        }
    }
}

internal object LanguageSettingsPanelInteraction {
    val contentOrder: List<String> = listOf("language-dropdown")

    fun options(
        state: LanguageUiState,
        textCatalogue: GemTextCatalogue,
    ): List<GemDropdownOption<LanguageOption>> =
        state.options.map { option ->
            when (option) {
                LanguageOption.Placeholder -> GemDropdownOption(
                    value = null,
                    label = textCatalogue.text(GemTextKey.ChooseLanguage),
                    enabled = false,
                    visualTone = GemDropdownOptionVisualTone.DISABLED,
                )
                LanguageOption.System -> GemDropdownOption(
                    value = option,
                    label = textCatalogue.text(GemTextKey.SystemLanguage),
                )
                is LanguageOption.Locale -> GemDropdownOption(
                    value = option,
                    label = option.nativeName,
                )
            }
        }

    fun selectedLabel(
        state: LanguageUiState,
        textCatalogue: GemTextCatalogue,
    ): String? =
        when (val preference = state.preference) {
            LanguagePreference.System -> textCatalogue.text(GemTextKey.SystemLanguage)
            is LanguagePreference.Locale -> state.options
                .filterIsInstance<LanguageOption.Locale>()
                .firstOrNull { it.localeTag == preference.localeTag.value }
                ?.nativeName
        }

    fun warningKey(state: LanguageUiState): GemTextKey? =
        state.warningKey
}
