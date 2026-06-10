package org.hostess.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(spacing.controlHeight),
        shape = HostessTheme.shapes.control,
        contentPadding = PaddingValues(horizontal = spacing.panelPadding),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.primaryInk,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        HostessButtonText(text)
    }
}

@Composable
fun HostessSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(spacing.controlHeight)
            .defaultMinSize(minWidth = spacing.controlHeight),
        shape = HostessTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(horizontal = spacing.panelPadding),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceStrong,
            contentColor = if (danger) colors.danger else colors.secondary,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        HostessButtonText(text)
    }
}

@Composable
fun HostessPlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(spacing.controlHeight)
            .defaultMinSize(minWidth = spacing.controlHeight),
        shape = HostessTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(horizontal = spacing.inlineGap),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceStrong,
            contentColor = colors.ink,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        HostessButtonText(text)
    }
}

@Composable
fun HostessIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
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
        shape = HostessTheme.shapes.control,
        border = BorderStroke(spacing.borderWidth, colors.lineStrong),
        contentPadding = PaddingValues(spacing.none),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceStrong,
            contentColor = colors.ink,
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        content()
    }
}

@Composable
private fun HostessButtonText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = HostessTheme.typeScale.button,
    )
}
