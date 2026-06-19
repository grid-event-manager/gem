package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class GemRgbSliderRowTest {
    @Test
    fun rgbLabelsUseSmallLabelTokens() {
        val colors = GemColors(
            muted = Color(0xFF102030),
            secondary = Color(0xFF405060),
        )
        val typeScale = GemTypeScale()

        assertEquals(Color(0xFF102030), GemRgbSliderRowInteraction.labelColor(colors))
        assertEquals(typeScale.smallLabel, GemRgbSliderRowInteraction.labelStyle(typeScale))
    }

    @Test
    fun numericUpdateKeepsValuesInsideRgbRange() {
        assertEquals(GemRgbNumericUpdate.Valid(value = 255, displayValue = "255"), GemRgbSliderRowInteraction.numericUpdate(10, "255"))
        assertEquals(GemRgbNumericUpdate.Valid(value = 0, displayValue = "0"), GemRgbSliderRowInteraction.numericUpdate(10, "0"))
        assertEquals(GemRgbNumericUpdate.Invalid(restoredDisplayValue = "10"), GemRgbSliderRowInteraction.numericUpdate(10, "256"))
    }
}
