package org.gem.ui.design

import androidx.compose.foundation.DefaultContextMenuRepresentation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun GemPlatformContextMenus(content: @Composable () -> Unit) {
    val colors = GemTheme.colors
    val representation = remember(colors) {
        DefaultContextMenuRepresentation(
            backgroundColor = colors.menuSurface,
            textColor = colors.secondary,
            itemHoverColor = colors.menuHover,
            disabledTextColor = colors.menuDisabledInk,
        )
    }
    CompositionLocalProvider(LocalContextMenuRepresentation provides representation) {
        content()
    }
}
