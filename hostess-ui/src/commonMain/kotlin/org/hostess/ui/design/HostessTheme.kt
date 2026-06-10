package org.hostess.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class HostessDesignTokens(
    val colors: HostessColors = HostessColors(),
    val spacing: HostessSpacing = HostessSpacing(),
    val shapes: HostessShapes = HostessShapes(),
    val typeScale: HostessTypeScale = HostessTypeScale(),
)

private val LocalHostessDesignTokens = staticCompositionLocalOf { HostessDesignTokens() }

object HostessTheme {
    @Composable
    fun Provide(
        tokens: HostessDesignTokens = HostessDesignTokens(),
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(LocalHostessDesignTokens provides tokens) {
            content()
        }
    }

    val colors: HostessColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHostessDesignTokens.current.colors

    val spacing: HostessSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalHostessDesignTokens.current.spacing

    val shapes: HostessShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalHostessDesignTokens.current.shapes

    val typeScale: HostessTypeScale
        @Composable
        @ReadOnlyComposable
        get() = LocalHostessDesignTokens.current.typeScale
}
