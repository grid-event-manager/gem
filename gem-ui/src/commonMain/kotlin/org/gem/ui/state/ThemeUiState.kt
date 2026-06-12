package org.gem.ui.state

import org.gem.core.theme.ThemePreference
import org.gem.ui.design.ResolvedThemeMode
import org.gem.ui.text.GemTextKey

data class ThemeUiState(
    val preference: ThemePreference = ThemePreference.SYSTEM,
    val resolvedMode: ResolvedThemeMode,
    val errorKey: GemTextKey? = null,
) {
    val toggleChecked: Boolean
        get() = resolvedMode == ResolvedThemeMode.DARK
}
