package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTheme
import org.gem.ui.design.GemTypeScale

@Composable
fun ThemeModeToggle(
    checked: Boolean,
    lightLabel: String,
    darkLabel: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = GemTheme.spacing
    val colors = GemTheme.colors
    val labelColor = ThemeModeToggleInteraction.labelColor(colors, enabled)
    val labelStyle = ThemeModeToggleInteraction.labelStyle(GemTheme.typeScale)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lightLabel,
            color = labelColor,
            style = labelStyle,
        )
        Box(
            modifier = Modifier
                .size(width = spacing.themeToggleWidth, height = spacing.themeToggleHeight)
                .background(
                    color = if (checked) colors.toggleTrackSelected else colors.toggleTrack,
                    shape = GemTheme.shapes.pill,
                )
                .border(
                    border = BorderStroke(spacing.borderWidth, colors.toggleBorder),
                    shape = GemTheme.shapes.pill,
                )
                .clickable(enabled = enabled, role = Role.Switch) {
                    onCheckedChange(ThemeModeToggleInteraction.toggledValue(checked))
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(
                        x = if (checked) {
                            spacing.themeToggleKnobCheckedOffset
                        } else {
                            spacing.themeToggleKnobInset
                        },
                        y = spacing.none,
                    )
                    .size(spacing.themeToggleKnobSize)
                    .background(colors.toggleKnob, GemTheme.shapes.pill),
            )
        }
        Text(
            text = darkLabel,
            color = labelColor,
            style = labelStyle,
        )
    }
}

internal object ThemeModeToggleInteraction {
    fun toggledValue(checked: Boolean): Boolean =
        !checked

    fun labelColor(
        colors: GemColors,
        enabled: Boolean,
    ): Color =
        if (enabled) colors.themeToggleLabelInk else colors.disabledInk

    fun labelStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.themeToggleLabel
}
