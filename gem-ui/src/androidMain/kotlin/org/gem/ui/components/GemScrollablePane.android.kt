package org.gem.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

actual val GemPlatformVisibleScrollbars: Boolean = false

@Composable
internal actual fun GemPlatformVerticalScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    hoverThumbColor: Color,
    modifier: Modifier,
) = Unit
