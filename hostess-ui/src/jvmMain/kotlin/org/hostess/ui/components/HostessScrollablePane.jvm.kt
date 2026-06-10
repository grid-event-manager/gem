package org.hostess.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.hostess.ui.design.HostessTheme

actual val HostessPlatformVisibleScrollbars: Boolean = true

@Composable
internal actual fun HostessPlatformVerticalScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    hoverThumbColor: Color,
    modifier: Modifier,
) {
    val spacing = HostessTheme.spacing
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = ScrollbarStyle(
            minimalHeight = spacing.scrollbarMinThumbHeight,
            thickness = spacing.scrollbarThickness,
            shape = RoundedCornerShape(spacing.scrollbarRadius),
            hoverDurationMillis = 120,
            unhoverColor = thumbColor,
            hoverColor = hoverThumbColor,
        ),
    )
}
