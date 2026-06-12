package org.gem.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class GemDesignTokens(
    val colors: GemColors = GemColors(),
    val spacing: GemSpacing = GemSpacing(),
    val shapes: GemShapes = GemShapes(),
    val typeScale: GemTypeScale = GemTypeScale(),
)

private val LocalGemDesignTokens = staticCompositionLocalOf { GemDesignTokens() }

object GemTheme {
    @Composable
    fun Provide(
        tokens: GemDesignTokens = GemDesignTokens(),
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(LocalGemDesignTokens provides tokens) {
            GemPlatformContextMenus {
                content()
            }
        }
    }

    val colors: GemColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGemDesignTokens.current.colors

    val spacing: GemSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalGemDesignTokens.current.spacing

    val shapes: GemShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalGemDesignTokens.current.shapes

    val typeScale: GemTypeScale
        @Composable
        @ReadOnlyComposable
        get() = LocalGemDesignTokens.current.typeScale
}
