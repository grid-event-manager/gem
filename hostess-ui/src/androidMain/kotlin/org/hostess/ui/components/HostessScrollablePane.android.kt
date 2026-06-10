package org.hostess.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

actual val HostessPlatformVisibleScrollbars: Boolean = false

@Composable
internal actual fun HostessPlatformVerticalScrollbar(
    scrollState: ScrollState,
    thumbColor: Color,
    hoverThumbColor: Color,
    modifier: Modifier,
) = Unit
