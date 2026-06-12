package org.hostess.ui.components

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
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessMenuIcon(
    modifier: Modifier = Modifier,
    tint: Color = HostessTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
fun HostessBackIcon(
    modifier: Modifier = Modifier,
    tint: Color = HostessTheme.colors.secondary,
) {
    Icon(
        imageVector = HostessBackArrowVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(HostessTheme.spacing.backIconSize),
    )
}

@Composable
fun HostessBrandLogoIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = HostessBrandLogoVector,
        contentDescription = null,
        tint = HostessTheme.colors.brandAccent,
        modifier = modifier,
    )
}

@Composable
fun HostessLandmarkIcon(
    modifier: Modifier = Modifier,
    tint: Color = HostessTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.LocationOn,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(HostessTheme.spacing.inlineIconSize),
    )
}

@Composable
fun HostessTextureIcon(
    modifier: Modifier = Modifier,
    tint: Color = HostessTheme.colors.secondary,
) {
    Icon(
        imageVector = HostessTextureVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(HostessTheme.spacing.inlineIconSize),
    )
}

@Composable
fun HostessClearIcon(
    modifier: Modifier = Modifier,
    tint: Color = HostessTheme.colors.secondary,
) {
    Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(HostessTheme.spacing.inlineIconSize),
    )
}

private const val HOSTESS_BRAND_LOGO_SOURCE_PATH: String =
    "M 40 200 C 40 200, 40 35, 100 35 C 160 35, 160 200, 160 200 C 160 200, 160 110, 100 110 C 40 110, 40 200, 40 200"

private val HostessBrandLogoVector: ImageVector =
    ImageVector.Builder(
        name = HOSTESS_BRAND_LOGO_SOURCE_PATH,
        defaultWidth = Dp(200f),
        defaultHeight = Dp(220f),
        viewportWidth = 200f,
        viewportHeight = 220f,
    ).apply {
        path(
            fill = null,
            stroke = SolidBrush(Color.Black),
            strokeLineWidth = 14f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(40f, 200f)
            curveTo(40f, 200f, 40f, 35f, 100f, 35f)
            curveTo(160f, 35f, 160f, 200f, 160f, 200f)
            curveTo(160f, 200f, 160f, 110f, 100f, 110f)
            curveTo(40f, 110f, 40f, 200f, 40f, 200f)
        }
    }.build()

private val HostessBackArrowVector: ImageVector =
    ImageVector.Builder(
        name = "HostessBackArrow",
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

private val HostessTextureVector: ImageVector =
    ImageVector.Builder(
        name = "HostessTexture",
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
