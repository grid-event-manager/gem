package org.gem.ui.components

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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

    @Test
    fun compactFieldsUseOwnedColoursAndSmallLabelTextStyle() {
        val colors = GemColors(
            ink = Color(0xFF010203),
            muted = Color(0xFF102030),
            disabledInk = Color(0xFF203040),
            danger = Color(0xFF304050),
            fieldBorder = Color(0xFF405060),
            line = Color(0xFF506070),
            primary = Color(0xFF607080),
        )
        val typeScale = GemTypeScale()

        assertEquals(Color(0xFF010203), GemCompactFieldTokens.textColor(colors, enabled = true, invalid = false))
        assertEquals(Color(0xFF203040), GemCompactFieldTokens.textColor(colors, enabled = false, invalid = false))
        assertEquals(Color(0xFF304050), GemCompactFieldTokens.textColor(colors, enabled = true, invalid = true))
        assertEquals(Color(0xFF102030), GemCompactFieldTokens.placeholderColor(colors))
        assertEquals(Color(0xFF405060), GemCompactFieldTokens.borderColor(colors, enabled = true, invalid = false))
        assertEquals(Color(0xFF506070), GemCompactFieldTokens.borderColor(colors, enabled = false, invalid = false))
        assertEquals(Color(0xFF304050), GemCompactFieldTokens.borderColor(colors, enabled = true, invalid = true))
        assertEquals(Color(0xFF607080), GemCompactFieldTokens.cursorColor(colors, invalid = false))
        assertEquals(Color(0xFF304050), GemCompactFieldTokens.cursorColor(colors, invalid = true))
        assertEquals(
            typeScale.smallLabel.copy(color = Color(0xFF010203), textAlign = TextAlign.Center),
            GemCompactFieldTokens.textStyle(typeScale, Color(0xFF010203), TextAlign.Center),
        )
        assertEquals(Alignment.CenterStart, GemCompactFieldTokens.contentAlignment(TextAlign.Start))
        assertEquals(Alignment.CenterStart, GemCompactFieldTokens.contentAlignment(TextAlign.Left))
        assertEquals(Alignment.CenterEnd, GemCompactFieldTokens.contentAlignment(TextAlign.End))
        assertEquals(Alignment.CenterEnd, GemCompactFieldTokens.contentAlignment(TextAlign.Right))
        assertEquals(Alignment.Center, GemCompactFieldTokens.contentAlignment(TextAlign.Center))
    }
}
