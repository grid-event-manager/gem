package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import org.gem.core.appearance.AppearanceColor

internal fun AppearanceColor.toComposeColor(): Color {
    val rgb = value.removePrefix("#").toLong(radix = 16)
    return Color(0xFF000000 or rgb)
}
