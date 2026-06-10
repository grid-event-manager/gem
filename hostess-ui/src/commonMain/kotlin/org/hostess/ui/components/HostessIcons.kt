package org.hostess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
