package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.gem.ui.design.GemTheme
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey
import org.gem.ui.time.SecondLifeTimeDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun GemPlatformTopBarChrome(
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
) {
    TopAppBar(
        modifier = modifier.heightIn(min = GemTheme.spacing.topBarMinHeight),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GemTheme.colors.topBar,
            titleContentColor = GemTheme.colors.topBarInk,
            actionIconContentColor = GemTheme.colors.topBarMenuInk,
            navigationIconContentColor = GemTheme.colors.brandAccent,
        ),
        navigationIcon = {
            GemBrandLogoIcon(
                modifier = Modifier.size(GemTheme.spacing.brandLogoSize),
            )
        },
        title = {
            Column {
                Text(
                    text = textCatalogue.text(GemTextKey.BrandInitials),
                    style = GemTheme.typeScale.brandTitle,
                    color = GemTheme.colors.brandInitialsInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = textCatalogue.text(GemTextKey.BrandSubtitle),
                    style = GemTheme.typeScale.brandSubtitle,
                    color = GemTheme.colors.brandWordmark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.secondLifeTimeMenuGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = secondLifeTimeDisplay.label(textCatalogue),
                    modifier = Modifier
                        .widthIn(min = GemTheme.spacing.secondLifeTimeMinWidth)
                        .testTag(GemTestTags.SecondLifeTime),
                    style = GemTheme.typeScale.smallLabel,
                    color = GemTheme.colors.topBarClockInk,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box {
                    GemTopBarIconButton(
                        onClick = onMenuClick,
                        contentDescription = textCatalogue.text(GemTextKey.Menu),
                        modifier = Modifier.testTag(GemTestTags.MenuButton),
                    ) {
                        GemMenuIcon(tint = GemTheme.colors.topBarMenuInk)
                    }
                    GemOverflowMenu(
                        expanded = menuOpen,
                        logoutEnabled = activeAccountLabel.isNotBlank(),
                        textCatalogue = textCatalogue,
                        onDismiss = onMenuDismiss,
                        onSettingsClick = onSettingsClick,
                        onLogoutClick = onLogoutClick,
                        onExitClick = onExitClick,
                    )
                }
            }
        },
    )
}
