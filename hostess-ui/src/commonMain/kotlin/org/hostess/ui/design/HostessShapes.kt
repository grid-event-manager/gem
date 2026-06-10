package org.hostess.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HostessShapes(
    val panelRadius: Dp = 8.dp,
    val controlRadius: Dp = 8.dp,
    val pillRadius: Dp = 999.dp,
) {
    val panel: Shape
        get() = RoundedCornerShape(panelRadius)

    val control: Shape
        get() = RoundedCornerShape(controlRadius)

    val pill: Shape
        get() = RoundedCornerShape(pillRadius)
}
