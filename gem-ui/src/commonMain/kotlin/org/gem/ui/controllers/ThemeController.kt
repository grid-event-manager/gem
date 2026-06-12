package org.gem.ui.controllers

import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadWarning
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.ui.design.ResolvedThemeMode
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.ThemeUiState
import org.gem.ui.text.GemTextKey

class ThemeController(
    private val runtime: GemUiRuntime,
    val state: ThemeUiState,
) {
    fun refresh(osDark: Boolean): ThemeController {
        val snapshot = runtime.themePreferenceService.loadPreference()
        return ThemeController(
            runtime = runtime,
            state = ThemeUiState(
                preference = snapshot.preference,
                resolvedMode = resolve(snapshot.preference, osDark),
                errorKey = when (snapshot.warning) {
                    null -> null
                    is ThemePreferenceLoadWarning.InvalidValue,
                    is ThemePreferenceLoadWarning.StorageFailed,
                    -> GemTextKey.ThemePreferenceUnavailable
                },
            ),
        )
    }

    fun setManualTheme(
        mode: ResolvedThemeMode,
        osDark: Boolean,
    ): ThemeController {
        val preference = when (mode) {
            ResolvedThemeMode.LIGHT -> ThemePreference.LIGHT
            ResolvedThemeMode.DARK -> ThemePreference.DARK
        }
        return when (runtime.themePreferenceService.savePreference(preference)) {
            ThemePreferenceSaveResult.Saved -> ThemeController(
                runtime = runtime,
                state = ThemeUiState(
                    preference = preference,
                    resolvedMode = resolve(preference, osDark),
                    errorKey = null,
                ),
            )
            is ThemePreferenceSaveResult.StorageFailed -> ThemeController(
                runtime = runtime,
                state = state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
            )
        }
    }

    companion object {
        fun initial(
            runtime: GemUiRuntime,
            osDark: Boolean,
        ): ThemeController =
            ThemeController(
                runtime = runtime,
                state = ThemeUiState(resolvedMode = resolve(ThemePreference.SYSTEM, osDark)),
            ).refresh(osDark)

        private fun resolve(
            preference: ThemePreference,
            osDark: Boolean,
        ): ResolvedThemeMode =
            when (preference) {
                ThemePreference.SYSTEM -> if (osDark) ResolvedThemeMode.DARK else ResolvedThemeMode.LIGHT
                ThemePreference.LIGHT -> ResolvedThemeMode.LIGHT
                ThemePreference.DARK -> ResolvedThemeMode.DARK
            }
    }
}
