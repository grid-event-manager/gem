package org.hostess.ui.controllers

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadWarning
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.ui.design.ResolvedThemeMode
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.ThemeUiState
import org.hostess.ui.text.HostessTextKey

class ThemeController(
    private val runtime: HostessUiRuntime,
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
                    -> HostessTextKey.ThemePreferenceUnavailable
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
                state = state.copy(errorKey = HostessTextKey.ThemePreferenceSaveFailed),
            )
        }
    }

    companion object {
        fun initial(
            runtime: HostessUiRuntime,
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
