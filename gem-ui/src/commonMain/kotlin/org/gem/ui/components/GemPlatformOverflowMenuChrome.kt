package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.text.GemTextCatalogue

@Composable
internal expect fun GemPlatformOverflowMenuChrome(
    expanded: Boolean,
    logoutEnabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier,
)
