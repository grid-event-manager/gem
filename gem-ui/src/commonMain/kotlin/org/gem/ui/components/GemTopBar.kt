package org.gem.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.navigation.AppMenuCommand
import org.gem.ui.navigation.AppMenuEntry
import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey
import org.gem.ui.time.SecondLifeTimeDisplay

data class GemTopBarTitle(
    val titleKey: GemTextKey,
    val subtitle: GemTopBarSubtitle,
) {
    companion object {
        fun brand(): GemTopBarTitle =
            GemTopBarTitle(
                titleKey = GemTextKey.BrandInitials,
                subtitle = GemTopBarSubtitle.Catalogue(GemTextKey.BrandSubtitle),
            )
    }
}

sealed interface GemTopBarSubtitle {
    data object None : GemTopBarSubtitle
    data class Catalogue(val key: GemTextKey) : GemTopBarSubtitle
    data class Data(val value: String) : GemTopBarSubtitle
}

@Composable
fun GemTopBar(
    title: GemTopBarTitle,
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
        title = title,
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
