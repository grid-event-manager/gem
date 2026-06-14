package org.gem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.design.GemTheme
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
internal actual fun GemPlatformOverflowMenuChrome(
    expanded: Boolean,
    logoutEnabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = GemTheme.colors.menuSurface,
        modifier = modifier
            .background(GemTheme.colors.menuSurface)
            .testTag(GemTestTags.AppMenu),
    ) {
        GemPlatformMenuItem(
            text = textCatalogue.text(GemTextKey.Settings),
            onClick = onSettingsClick,
            modifier = Modifier.testTag(GemTestTags.OpenSettings),
        )
        GemPlatformMenuItem(
            text = textCatalogue.text(GemTextKey.LogOut),
            onClick = onLogoutClick,
            enabled = logoutEnabled,
            modifier = Modifier.testTag(GemTestTags.LogOut),
        )
        HorizontalDivider(
            color = GemTheme.colors.lineStrong,
            thickness = GemTheme.spacing.borderWidth,
        )
        GemPlatformMenuItem(
            text = textCatalogue.text(GemTextKey.Exit),
            onClick = onExitClick,
            modifier = Modifier.testTag(GemTestTags.Exit),
        )
    }
}

@Composable
private fun GemPlatformMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = GemTheme.typeScale.menuItem,
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = spacing.menuItemHorizontalPadding,
            vertical = spacing.menuItemVerticalPadding,
        ),
        colors = MenuDefaults.itemColors(
            textColor = colors.secondary,
            disabledTextColor = colors.menuDisabledInk,
        ),
    )
}
