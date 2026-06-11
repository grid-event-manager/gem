package org.hostess.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags

@Composable
fun SettingsBackNav(
    text: String,
    onBack: () -> Unit,
    themeChecked: Boolean,
    themeEnabled: Boolean,
    lightText: String,
    darkText: String,
    onThemeCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backInteraction = remember { MutableInteractionSource() }
    val backHovered by backInteraction.collectIsHoveredAsState()
    val backColor = if (backHovered) {
        HostessTheme.colors.interactiveHoverInk
    } else {
        HostessTheme.colors.secondary
    }
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .hoverable(backInteraction)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onBack,
                    )
                    .testTag(HostessTestTags.SettingsBack),
                horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HostessBackIcon(tint = backColor)
                Text(
                    text = text,
                    color = backColor,
                    style = HostessTheme.typeScale.smallLabel,
                )
            }
            ThemeModeToggle(
                checked = themeChecked,
                enabled = themeEnabled,
                lightLabel = lightText,
                darkLabel = darkText,
                onCheckedChange = onThemeCheckedChange,
            )
        }
        HorizontalDivider(
            color = HostessTheme.colors.line,
            thickness = HostessTheme.spacing.borderWidth,
        )
    }
}
