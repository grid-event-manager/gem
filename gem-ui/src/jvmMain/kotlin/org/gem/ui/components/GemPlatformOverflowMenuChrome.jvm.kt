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
                    GemMenuRow(
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
                    GemMenuRow(
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
            style = GemMenuTextTokens.textStyle(GemTheme.typeScale),
            color = GemMenuTextTokens.textColor(colors, enabled),
        )
    }
}
