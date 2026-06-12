package org.gem.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor as SolidBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import org.gem.ui.design.GemTheme

@Composable
fun GemMenuIcon(
    modifier: Modifier = Modifier,
    tint: Color = GemTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
fun GemBackIcon(
    modifier: Modifier = Modifier,
    tint: Color = GemTheme.colors.secondary,
) {
    Icon(
        imageVector = GemBackArrowVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(GemTheme.spacing.backIconSize),
    )
}

@Composable
fun GemBrandLogoIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = GemBrandLogoVector,
        contentDescription = null,
        tint = GemTheme.colors.brandAccent,
        modifier = modifier,
    )
}

@Composable
fun GemLandmarkIcon(
    modifier: Modifier = Modifier,
    tint: Color = GemTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.LocationOn,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(GemTheme.spacing.inlineIconSize),
    )
}

@Composable
fun GemTextureIcon(
    modifier: Modifier = Modifier,
    tint: Color = GemTheme.colors.secondary,
) {
    Icon(
        imageVector = GemTextureVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(GemTheme.spacing.inlineIconSize),
    )
}

@Composable
fun GemClearIcon(
    modifier: Modifier = Modifier,
    tint: Color = GemTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(GemTheme.spacing.inlineIconSize),
    )
}

private val GemBrandLogoVector: ImageVector =
    ImageVector.Builder(
        name = "GemBrandDiamond",
        defaultWidth = Dp(24f),
        defaultHeight = Dp(24f),
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidBrush(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 4f)
            lineTo(17f, 4f)
            lineTo(21f, 9f)
            lineTo(12f, 21f)
            lineTo(3f, 9f)
            close()
            moveTo(3f, 9f)
            lineTo(21f, 9f)
            moveTo(7f, 4f)
            lineTo(9.5f, 9f)
            lineTo(12f, 21f)
            moveTo(17f, 4f)
            lineTo(14.5f, 9f)
            lineTo(12f, 21f)
            moveTo(9.5f, 9f)
            lineTo(12f, 4f)
            lineTo(14.5f, 9f)
        }
    }.build()

private val GemBackArrowVector: ImageVector =
    ImageVector.Builder(
        name = "GemBackArrow",
        defaultWidth = Dp(24f),
        defaultHeight = Dp(24f),
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidBrush(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(21f, 12f)
            lineTo(4f, 12f)
            moveTo(8f, 8f)
            lineTo(4f, 12f)
            lineTo(8f, 16f)
        }
    }.build()

private val GemTextureVector: ImageVector =
    ImageVector.Builder(
        name = "GemTexture",
        defaultWidth = Dp(24f),
        defaultHeight = Dp(24f),
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidBrush(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 19f)
            lineTo(5f, 19f)
            close()
            moveTo(8f, 16f)
            lineTo(11f, 12f)
            lineTo(14f, 15f)
            lineTo(16f, 12f)
            lineTo(19f, 16f)
            moveTo(9f, 9f)
            lineTo(9.1f, 9f)
        }
    }.build()
