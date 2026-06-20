package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.gem.core.appearance.AppearanceProfileId
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.text.AppearanceProfileDisplayLabel
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun AppearanceThemesPanel(
    state: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
    onProfileSelected: (AppearanceProfileId) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GemUnlabelledDropdownField(
        selectedLabel = AppearanceThemesPanelInteraction.selectedProfileLabel(state, textCatalogue),
        placeholderLabel = textCatalogue.text(GemTextKey.Themes),
        options = AppearanceThemesPanelInteraction.profileOptions(state, textCatalogue),
        onSelected = { selected -> selected?.let(onProfileSelected) },
        enabled = enabled,
        modifier = modifier,
        textAlign = TextAlign.Center,
    )
}

internal object AppearanceThemesPanelInteraction {
    val contentOrder: List<String> = listOf("theme-dropdown")

    fun profileOptions(
        state: AppearanceUiState,
        textCatalogue: GemTextCatalogue,
    ): List<GemDropdownOption<AppearanceProfileId>> =
        listOf<GemDropdownOption<AppearanceProfileId>>(
            GemDropdownOption(
                null,
                textCatalogue.text(GemTextKey.ChooseTheme),
                enabled = false,
                visualTone = GemDropdownOptionVisualTone.DISABLED,
            ),
        ) + state.profiles.map { profile ->
            GemDropdownOption(
                profile.id,
                AppearanceProfileDisplayLabel.profile(profile, textCatalogue),
            )
        }

    fun selectedProfileLabel(
        state: AppearanceUiState,
        textCatalogue: GemTextCatalogue,
    ): String? =
        state.profiles
            .firstOrNull { it.id == state.selectedProfileId }
            ?.let { AppearanceProfileDisplayLabel.profile(it, textCatalogue) }

    fun hasSaveOrResetControls(): Boolean = false
}
