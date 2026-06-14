package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GemTheme.spacing.topBarMinHeight),
        color = GemTheme.colors.topBar,
        contentColor = GemTheme.colors.topBarInk,
        shape = GemTheme.shapes.panel,
    ) {
        Row(
            modifier = Modifier.padding(GemTheme.spacing.panelPadding),
            horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.inlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GemBrandLogoIcon(
                modifier = Modifier.size(GemTheme.spacing.brandLogoSize),
            )
            Column(modifier = Modifier.weight(weight = 1f)) {
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
    }
}
