package org.hostess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun HostessOverflowMenu(
    expanded: Boolean,
    textCatalogue: HostessTextCatalogue,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = HostessTheme.colors.menuSurface,
        modifier = modifier
            .background(HostessTheme.colors.menuSurface)
            .testTag(HostessTestTags.AppMenu),
    ) {
        HostessMenuRow(
            text = textCatalogue.text(HostessTextKey.Settings),
            onClick = onSettingsClick,
            modifier = Modifier.testTag(HostessTestTags.OpenSettings),
        )
        HostessMenuRow(
            text = textCatalogue.text(HostessTextKey.LogOut),
            onClick = onLogoutClick,
            modifier = Modifier.testTag(HostessTestTags.LogOut),
        )
    }
}

@Composable
private fun HostessMenuRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.menuItemMinHeight)
            .background(colors.menuSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = spacing.menuItemHorizontalPadding,
                vertical = spacing.menuItemVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = HostessTheme.typeScale.menuItem,
            color = colors.secondary,
        )
    }
}
