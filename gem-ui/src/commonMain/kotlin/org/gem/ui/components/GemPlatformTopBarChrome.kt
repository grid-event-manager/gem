package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.time.SecondLifeTimeDisplay

@Composable
internal expect fun GemPlatformTopBarChrome(
    activeAccountLabel: String,
    secondLifeTimeDisplay: SecondLifeTimeDisplay,
    menuOpen: Boolean,
    textCatalogue: GemTextCatalogue,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier,
)
