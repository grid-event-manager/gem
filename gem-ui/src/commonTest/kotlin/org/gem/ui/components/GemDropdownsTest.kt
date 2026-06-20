package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class GemDropdownsTest {
    @Test
    fun collapsedDropdownTextUsesFieldTextRoute() {
        val colors = GemColors(
            ink = Color(0xFF102030),
            muted = Color(0xFF405060),
        )
        val typeScale = GemTypeScale()

        assertEquals(Color(0xFF102030), GemDropdownTokens.selectorTextColor(colors, hasSelection = true))
        assertEquals(Color(0xFF405060), GemDropdownTokens.selectorTextColor(colors, hasSelection = false))
        assertEquals(typeScale.fieldText, GemDropdownTokens.selectorTextStyle(typeScale))
    }

    @Test
    fun dropdownMenuRowsUseMenuAndDisabledTokens() {
        val colors = GemColors(
            topBarMenuInk = Color(0xFF102030),
            menuDisabledInk = Color(0xFF405060),
        )
        val typeScale = GemTypeScale()

        assertEquals(Color(0xFF102030), GemMenuTextTokens.textColor(colors, enabled = true))
        assertEquals(Color(0xFF405060), GemMenuTextTokens.textColor(colors, enabled = false))
        assertEquals(
            Color(0xFF405060),
            GemDropdownTokens.menuTextColor(colors, GemDropdownOptionVisualTone.PLACEHOLDER),
        )
        assertEquals(typeScale.menuItem, GemMenuTextTokens.textStyle(typeScale))
    }

    @Test
    fun dropdownOptionVisualToneDefaultsToClickability() {
        assertEquals(GemDropdownOptionVisualTone.DEFAULT, GemDropdownOption("value", "Value").visualTone)
        assertEquals(GemDropdownOptionVisualTone.DISABLED, GemDropdownOption<String>(null, "Label", enabled = false).visualTone)
        assertEquals(
            GemDropdownOptionVisualTone.PLACEHOLDER,
            GemDropdownOption<String>(null, "Label", visualTone = GemDropdownOptionVisualTone.PLACEHOLDER).visualTone,
        )
    }
}
