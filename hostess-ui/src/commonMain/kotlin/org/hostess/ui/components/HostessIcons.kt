package org.hostess.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun HostessMenuIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun HostessBackIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = null,
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
            strokeLineWidth = 10f,
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
