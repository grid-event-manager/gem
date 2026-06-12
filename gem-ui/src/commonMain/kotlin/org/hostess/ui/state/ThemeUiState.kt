package org.hostess.ui.state

import org.hostess.core.theme.ThemePreference
import org.hostess.ui.design.ResolvedThemeMode
import org.hostess.ui.text.HostessTextKey

data class ThemeUiState(
    val preference: ThemePreference = ThemePreference.SYSTEM,
    val resolvedMode: ResolvedThemeMode,
    val errorKey: HostessTextKey? = null,
) {
    val toggleChecked: Boolean
        get() = resolvedMode == ResolvedThemeMode.DARK
}
