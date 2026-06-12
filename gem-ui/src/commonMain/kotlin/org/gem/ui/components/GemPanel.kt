package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.design.GemTheme

@Composable
fun GemPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (strong) colors.surfaceStrong else colors.surface,
        contentColor = colors.ink,
        shape = GemTheme.shapes.panel,
        border = BorderStroke(spacing.borderWidth, colors.line),
    ) {
        Column(
            modifier = Modifier.padding(spacing.panelPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
            content = content,
        )
    }
}
