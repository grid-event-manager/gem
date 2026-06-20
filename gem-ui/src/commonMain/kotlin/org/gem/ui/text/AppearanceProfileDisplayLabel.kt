package org.gem.ui.text

import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.ui.state.AppearanceUiState

object AppearanceProfileDisplayLabel {
    fun current(
        state: AppearanceUiState,
        textCatalogue: GemTextCatalogue,
    ): String {
        val selectedProfile = state.profiles.firstOrNull { it.id == state.selectedProfileId }
        return selectedProfile
            ?.let { profile(it, textCatalogue) }
            ?: default(state.mode, textCatalogue)
    }

    fun profile(
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

    private fun default(
        mode: AppearanceMode,
        textCatalogue: GemTextCatalogue,
    ): String =
        "${textCatalogue.text(GemTextKey.GemDefault)} ${textCatalogue.text(mode.labelKey())}"

    private fun AppearanceMode.labelKey(): GemTextKey =
        when (this) {
            AppearanceMode.LIGHT -> GemTextKey.Light
            AppearanceMode.DARK -> GemTextKey.Dark
        }
}
