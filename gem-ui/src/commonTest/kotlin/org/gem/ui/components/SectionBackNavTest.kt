package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class SectionBackNavTest {
    @Test
    fun backTextUsesBackAndHoverTokens() {
        val colors = GemColors(
            navigationInk = Color(0xFF102030),
            interactiveHoverInk = Color(0xFF405060),
        )
        val typeScale = GemTypeScale(
            backLabel = TextStyle(fontSize = 13.sp),
        )

        assertEquals(Color(0xFF102030), SectionBackNavTokens.backColor(colors, hovered = false))
        assertEquals(Color(0xFF405060), SectionBackNavTokens.backColor(colors, hovered = true))
        assertEquals(typeScale.backLabel, SectionBackNavTokens.backLabelStyle(typeScale))
    }
}
