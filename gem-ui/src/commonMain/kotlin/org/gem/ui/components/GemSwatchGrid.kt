package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.gem.core.appearance.AppearanceColor
import org.gem.ui.design.AppearanceSwatchPalette
import org.gem.ui.design.GemTheme
import org.gem.ui.design.toComposeColor

@Composable
fun GemSwatchGrid(
    onSwatchSelected: (AppearanceColor) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<AppearanceColor> = AppearanceSwatchPalette.colors,
    columns: Int = AppearanceSwatchPalette.Columns,
) {
    val spacing = GemTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.appearanceSwatchGap),
    ) {
        GemSwatchGridLayout.rows(colors, columns).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.appearanceSwatchGap),
            ) {
                rowColors.forEach { color ->
                    GemSwatchCell(
                        color = color,
                        onClick = { onSwatchSelected(color) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GemSwatchCell(
    color: AppearanceColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = GemTheme.spacing.appearanceSwatchMinHeight)
            .aspectRatio(1f)
            .clickable(role = Role.Button, onClick = onClick),
        shape = GemTheme.shapes.swatch,
        color = color.toComposeColor(),
        border = BorderStroke(GemTheme.spacing.borderWidth, GemTheme.colors.fieldBorder),
    ) {
    }
}

internal object GemSwatchGridLayout {
    fun rows(
        colors: List<AppearanceColor>,
        columns: Int,
    ): List<List<AppearanceColor>> {
        require(columns > 0) { "Swatch columns must be positive." }
        return colors.chunked(columns)
    }
}
