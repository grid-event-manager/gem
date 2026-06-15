package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.navigation.AppMenuCommand
import org.gem.ui.navigation.AppMenuEntry
import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.time.SecondLifeTimeDisplay

@Composable
fun GemTopBar(
    activeAccountLabel: String,
    secondLifeTimeDisplay: SecondLifeTimeDisplay,
    menuOpen: Boolean,
    menuEntries: List<AppMenuEntry>,
    textCatalogue: GemTextCatalogue,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onMenuSectionSelected: (UiRoute) -> Unit,
    onMenuCommandSelected: (AppMenuCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPlatformTopBarChrome(
        activeAccountLabel = activeAccountLabel,
        secondLifeTimeDisplay = secondLifeTimeDisplay,
        menuOpen = menuOpen,
        menuEntries = menuEntries,
        textCatalogue = textCatalogue,
        onMenuClick = onMenuClick,
        onMenuDismiss = onMenuDismiss,
        onMenuSectionSelected = onMenuSectionSelected,
        onMenuCommandSelected = onMenuCommandSelected,
        modifier = modifier,
    )
}
