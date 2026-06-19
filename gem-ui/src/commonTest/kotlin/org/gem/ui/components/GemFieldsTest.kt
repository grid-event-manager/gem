package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class GemFieldsTest {
    @Test
    fun fieldLabelsAndInputsUseTheirOwnedTextTokens() {
        val colors = GemColors(
            muted = Color(0xFF102030),
            secondary = Color(0xFF405060),
        )
        val typeScale = GemTypeScale()

        assertEquals(Color(0xFF102030), GemFieldTokens.labelColor(colors))
        assertEquals(typeScale.smallLabel, GemFieldTokens.labelStyle(typeScale))
        assertEquals(typeScale.fieldText, GemFieldTokens.inputTextStyle(typeScale))
    }
}
