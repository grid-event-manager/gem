package org.hostess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessMenuIcon(modifier: Modifier = Modifier) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
    ) {
        repeat(MenuBarCount) {
            Box(
                modifier = Modifier
                    .width(spacing.rowIconSize)
                    .height(spacing.borderWidth)
                    .background(colors.ink),
            )
        }
    }
}

private const val MenuBarCount = 3

@Composable
fun HostessBackIcon(modifier: Modifier = Modifier) {
    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    Canvas(modifier = modifier.size(spacing.inlineGap)) {
        drawLine(
            color = colors.ink,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height / BackIconMidpoint),
            strokeWidth = spacing.borderWidth.toPx(),
        )
        drawLine(
            color = colors.ink,
            start = Offset(0f, size.height / BackIconMidpoint),
            end = Offset(size.width, size.height),
            strokeWidth = spacing.borderWidth.toPx(),
        )
    }
}

private const val BackIconMidpoint = 2f
