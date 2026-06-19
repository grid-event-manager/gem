package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale

internal object GemMenuTextTokens {
    fun textColor(
        colors: GemColors,
        enabled: Boolean,
    ): Color =
        if (enabled) colors.topBarMenuInk else colors.menuDisabledInk

    fun textStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.menuItem
}
