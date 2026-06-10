package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun HostessTopBar(
    activeAccountLabel: String,
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
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = textCatalogue.text(HostessTextKey.AppName),
                    style = HostessTheme.typeScale.brandTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (activeAccountLabel.isNotBlank()) {
                    Text(
                        text = activeAccountLabel,
                        style = HostessTheme.typeScale.smallLabel,
                        color = HostessTheme.colors.topBarInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HostessIconButton(
                onClick = onMenuClick,
                contentDescription = textCatalogue.text(HostessTextKey.Menu),
                modifier = Modifier.testTag(HostessTestTags.MenuButton),
            ) {
                HostessMenuIcon()
            }
            HostessOverflowMenu(
                expanded = menuOpen,
                textCatalogue = textCatalogue,
                onDismiss = onMenuDismiss,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick,
            )
        }
    }
}
