package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import org.gem.ui.design.GemTheme

@Composable
fun GemPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    visuallyEnabled: Boolean = enabled,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(spacing.controlHeight),
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(horizontal = spacing.panelPadding),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (visuallyEnabled) colors.fieldSurface else colors.disabledBackground,
            contentColor = if (visuallyEnabled) colors.buttonLabelInk else colors.disabledInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        GemButtonText(text)
    }
}

@Composable
fun GemSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(spacing.controlHeight)
            .defaultMinSize(minWidth = spacing.controlHeight),
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(horizontal = spacing.panelPadding),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.fieldSurface,
            contentColor = if (danger) colors.danger else colors.buttonLabelInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        GemButtonText(text)
    }
}

@Composable
fun GemPlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(spacing.controlHeight)
            .defaultMinSize(minWidth = spacing.controlHeight),
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(horizontal = spacing.inlineGap),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.fieldSurface,
            contentColor = colors.buttonLabelInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        GemButtonText(text)
    }
}

@Composable
fun GemStaticButtonSurface(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.controlHeight),
        shape = GemTheme.shapes.control,
        color = colors.fieldSurface,
        contentColor = colors.buttonLabelInk,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            GemButtonText(text)
        }
    }
}

@Composable
fun GemIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(spacing.tapTarget)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(spacing.none),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.fieldSurface,
            contentColor = colors.buttonLabelInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        content()
    }
}

@Composable
fun GemTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(spacing.tapTarget)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        shape = GemTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.topBarButtonBorder),
        contentPadding = PaddingValues(spacing.none),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.topBarButtonSurface,
            contentColor = colors.topBarMenuInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        content()
    }
}

@Composable
fun GemInlineIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val spacing = GemTheme.spacing
    Box(
        modifier = modifier
            .size(spacing.statusPillMinHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = contentDescription }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun GemButtonText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = GemTheme.typeScale.button,
    )
}
