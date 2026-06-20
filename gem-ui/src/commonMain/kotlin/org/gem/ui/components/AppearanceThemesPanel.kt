package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.ui.state.AppearanceUiState
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
        placeholderLabel = textCatalogue.text(GemTextKey.ChooseTheme),
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
            ),
        ) + state.profiles.map { profile ->
            GemDropdownOption(
                profile.id,
                profileLabel(profile, textCatalogue),
            )
        }

    fun selectedProfileLabel(
        state: AppearanceUiState,
        textCatalogue: GemTextCatalogue,
    ): String? =
        state.profiles
            .firstOrNull { it.id == state.selectedProfileId }
            ?.let { profileLabel(it, textCatalogue) }

    fun profileLabel(
        profile: AppearanceProfile,
        textCatalogue: GemTextCatalogue,
    ): String {
        val mode = textCatalogue.text(profile.mode.labelKey())
        return when (profile.source) {
            AppearanceProfileSource.STOCK -> "${profile.name.value} $mode"
            AppearanceProfileSource.CUSTOM -> "${profile.name.value} ($mode)"
            AppearanceProfileSource.SYSTEM -> error("System profiles are hidden and are not renderable options.")
        }
    }

    fun hasSaveOrResetControls(): Boolean = false
}

private fun org.gem.core.appearance.AppearanceMode.labelKey(): GemTextKey =
    when (this) {
        org.gem.core.appearance.AppearanceMode.LIGHT -> GemTextKey.Light
        org.gem.core.appearance.AppearanceMode.DARK -> GemTextKey.Dark
    }
