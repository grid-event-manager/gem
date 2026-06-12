package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import org.gem.ui.design.GemTheme

@Composable
fun GemSelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
    titleStyle: TextStyle = GemTheme.typeScale.body,
    leading: @Composable (RowScope.() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(
                min = if (compact) {
                    spacing.compactRowMinHeight
                } else {
                    spacing.sessionStripMinHeight
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) {
            colors.selectedBackground
        } else if (compact) {
            colors.fieldSurface
        } else {
            colors.surfaceStrong
        },
        contentColor = if (enabled) colors.ink else colors.disabledInk,
        border = BorderStroke(spacing.borderWidth, colors.line),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.rowHorizontalPadding,
                vertical = if (compact) spacing.compactRowVerticalPadding else spacing.rowVerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
            }
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = title,
                    color = if (enabled) colors.ink else colors.disabledInk,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = if (enabled) colors.muted else colors.disabledInk,
                        style = GemTheme.typeScale.smallLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}

@Composable
fun GemCheckboxCard(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.controlHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        shape = GemTheme.shapes.control,
        color = if (checked) colors.selectedBackground else colors.surfaceStrong,
        contentColor = colors.ink,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.fieldHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.lineStrong,
                    checkmarkColor = colors.primaryInk,
                    disabledCheckedColor = colors.disabledBackground,
                    disabledUncheckedColor = colors.line,
                ),
            )
            Text(
                text = text,
                color = if (enabled) colors.ink else colors.disabledInk,
                style = GemTheme.typeScale.button,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GemSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(spacing.controlHeight),
        shape = GemTheme.shapes.control,
        border = BorderStroke(
            width = spacing.borderWidth,
            color = if (selected) colors.primary else colors.lineStrong,
        ),
        contentPadding = PaddingValues(horizontal = spacing.fieldGap),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) colors.selectedBackground else colors.surfaceStrong,
            contentColor = if (selected) colors.selectedInk else colors.buttonLabelInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        Text(
            text = text,
            style = GemTheme.typeScale.button,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
