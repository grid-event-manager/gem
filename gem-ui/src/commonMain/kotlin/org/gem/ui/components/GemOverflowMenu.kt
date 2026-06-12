package org.gem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.gem.ui.design.GemTheme
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun GemOverflowMenu(
    expanded: Boolean,
    logoutEnabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = GemTheme.colors.menuSurface,
        modifier = modifier
            .background(GemTheme.colors.menuSurface)
            .testTag(GemTestTags.AppMenu),
    ) {
        GemMenuRow(
            text = textCatalogue.text(GemTextKey.Settings),
            onClick = onSettingsClick,
            modifier = Modifier.testTag(GemTestTags.OpenSettings),
        )
        HorizontalDivider(
            color = GemTheme.colors.line,
            thickness = GemTheme.spacing.borderWidth,
        )
        GemMenuRow(
            text = textCatalogue.text(GemTextKey.LogOut),
            onClick = onLogoutClick,
            enabled = logoutEnabled,
            modifier = Modifier.testTag(GemTestTags.LogOut),
        )
    }
}

@Composable
private fun GemMenuRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.menuItemMinHeight)
            .background(colors.menuSurface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                horizontal = spacing.menuItemHorizontalPadding,
                vertical = spacing.menuItemVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = GemTheme.typeScale.menuItem,
            color = if (enabled) colors.secondary else colors.menuDisabledInk,
        )
    }
}
