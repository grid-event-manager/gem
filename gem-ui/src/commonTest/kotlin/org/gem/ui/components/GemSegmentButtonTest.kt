package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import org.gem.ui.design.GemColors
import kotlin.test.Test
import kotlin.test.assertEquals

class GemSegmentButtonTest {
    @Test
    fun segmentButtonUsesButtonLabelInkForSelectedAndUnselectedContent() {
        val colors = GemColors(
            buttonLabelInk = Color(0xFF102030),
            selectedBackground = Color(0xFF203040),
            surfaceStrong = Color(0xFF304050),
            primary = Color(0xFF405060),
            lineStrong = Color(0xFF506070),
        )

        assertEquals(Color(0xFF102030), GemSegmentButtonTokens.contentInk(colors))
        assertEquals(Color(0xFF203040), GemSegmentButtonTokens.containerFill(colors, selected = true))
        assertEquals(Color(0xFF304050), GemSegmentButtonTokens.containerFill(colors, selected = false))
        assertEquals(Color(0xFF405060), GemSegmentButtonTokens.borderInk(colors, selected = true))
        assertEquals(Color(0xFF506070), GemSegmentButtonTokens.borderInk(colors, selected = false))
    }
}
