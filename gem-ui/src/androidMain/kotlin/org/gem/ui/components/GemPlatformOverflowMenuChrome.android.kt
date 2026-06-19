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
import org.gem.ui.navigation.AppMenuCommand
import org.gem.ui.navigation.AppMenuEntry
import org.gem.ui.state.UiRoute
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
internal actual fun GemPlatformOverflowMenuChrome(
    expanded: Boolean,
    menuEntries: List<AppMenuEntry>,
    textCatalogue: GemTextCatalogue,
    onDismiss: () -> Unit,
    onMenuSectionSelected: (UiRoute) -> Unit,
    onMenuCommandSelected: (AppMenuCommand) -> Unit,
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
        menuEntries.forEach { entry ->
            when (entry) {
                is AppMenuEntry.SectionEntry -> {
                    GemPlatformMenuItem(
                        text = textCatalogue.text(entry.section.labelKey),
                        onClick = { onMenuSectionSelected(entry.section.route) },
                        modifier = Modifier.testTag(entry.testTag),
                    )
                }
                is AppMenuEntry.CommandEntry -> {
                    if (entry.dividerBefore) {
                        HorizontalDivider(
                            color = GemTheme.colors.lineStrong,
                            thickness = GemTheme.spacing.borderWidth,
                        )
                    }
                    GemPlatformMenuItem(
                        text = textCatalogue.text(entry.labelKey),
                        onClick = { onMenuCommandSelected(entry.command) },
                        enabled = entry.enabled,
                        modifier = Modifier.testTag(entry.testTag),
                    )
                }
            }
        }
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
                style = GemMenuTextTokens.textStyle(GemTheme.typeScale),
                color = GemMenuTextTokens.textColor(colors, enabled),
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
            textColor = GemMenuTextTokens.textColor(colors, enabled = true),
            disabledTextColor = GemMenuTextTokens.textColor(colors, enabled = false),
        ),
    )
}
