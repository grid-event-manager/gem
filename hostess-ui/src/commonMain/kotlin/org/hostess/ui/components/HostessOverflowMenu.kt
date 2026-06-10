package org.hostess.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        modifier = modifier.testTag(HostessTestTags.AppMenu),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = textCatalogue.text(HostessTextKey.Settings),
                    style = HostessTheme.typeScale.button,
                )
            },
            onClick = onSettingsClick,
            modifier = Modifier.testTag(HostessTestTags.OpenSettings),
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = textCatalogue.text(HostessTextKey.LogOut),
                    style = HostessTheme.typeScale.button,
                    color = HostessTheme.colors.danger,
                )
            },
            onClick = onLogoutClick,
            modifier = Modifier.testTag(HostessTestTags.LogOut),
        )
    }
}
