package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.text.GemTextCatalogue

@Composable
fun GemOverflowMenu(
    expanded: Boolean,
    logoutEnabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPlatformOverflowMenuChrome(
        expanded = expanded,
        logoutEnabled = logoutEnabled,
        textCatalogue = textCatalogue,
        onDismiss = onDismiss,
        onSettingsClick = onSettingsClick,
        onLogoutClick = onLogoutClick,
        onExitClick = onExitClick,
        modifier = modifier,
    )
}
