package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.navigation.AppMenuCommand
import org.gem.ui.navigation.AppMenuEntry
import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextCatalogue

@Composable
internal expect fun GemPlatformOverflowMenuChrome(
    expanded: Boolean,
    menuEntries: List<AppMenuEntry>,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onMenuSectionSelected: (UiRoute) -> Unit,
    onMenuCommandSelected: (AppMenuCommand) -> Unit,
    modifier: Modifier,
)
