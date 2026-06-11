package org.hostess.ui.components

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
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import org.hostess.ui.time.SecondLifeTimeDisplay

@Composable
fun HostessTopBar(
    activeAccountLabel: String,
    secondLifeTimeDisplay: SecondLifeTimeDisplay,
    menuOpen: Boolean,
    textCatalogue: HostessTextCatalogue,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HostessTheme.spacing.topBarMinHeight),
        color = HostessTheme.colors.topBar,
        contentColor = HostessTheme.colors.topBarInk,
        shape = HostessTheme.shapes.panel,
    ) {
        Row(
            modifier = Modifier.padding(HostessTheme.spacing.panelPadding),
            horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.inlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HostessBrandLogoIcon(
                modifier = Modifier.size(HostessTheme.spacing.brandLogoSize),
            )
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = textCatalogue.text(HostessTextKey.BrandInitials),
                    style = HostessTheme.typeScale.brandTitle,
                    color = HostessTheme.colors.topBarInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = textCatalogue.text(HostessTextKey.BrandSubtitle),
                    style = HostessTheme.typeScale.smallLabel,
                    color = HostessTheme.colors.brandWordmark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = secondLifeTimeDisplay.label(textCatalogue),
                modifier = Modifier
                    .widthIn(min = HostessTheme.spacing.secondLifeTimeMinWidth)
                    .testTag(HostessTestTags.SecondLifeTime),
                style = HostessTheme.typeScale.smallLabel,
                color = HostessTheme.colors.topBarClockInk,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box {
                HostessTopBarIconButton(
                    onClick = onMenuClick,
                    contentDescription = textCatalogue.text(HostessTextKey.Menu),
                    modifier = Modifier.testTag(HostessTestTags.MenuButton),
                ) {
                    HostessMenuIcon(tint = HostessTheme.colors.topBarMenuInk)
                }
                HostessOverflowMenu(
                    expanded = menuOpen,
                    logoutEnabled = activeAccountLabel.isNotBlank(),
                    textCatalogue = textCatalogue,
                    onDismiss = onMenuDismiss,
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick,
                )
            }
        }
    }
}
