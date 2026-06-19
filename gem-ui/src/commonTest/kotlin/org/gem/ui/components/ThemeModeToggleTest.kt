package org.gem.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemSpacing
import org.gem.ui.design.GemTypeScale
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeToggleTest {
    @Test
    fun toggleInteractionFlipsCheckedState() {
        assertEquals(true, ThemeModeToggleInteraction.toggledValue(false))
        assertEquals(false, ThemeModeToggleInteraction.toggledValue(true))
    }

    @Test
    fun toggleUsesPinnedSharedDimensions() {
        val spacing = GemSpacing()

        assertEquals(36.dp, spacing.themeToggleWidth)
        assertEquals(20.dp, spacing.themeToggleHeight)
        assertEquals(14.dp, spacing.themeToggleKnobSize)
        assertEquals(3.dp, spacing.themeToggleKnobInset)
        assertEquals(16.dp, spacing.themeToggleKnobCheckedOffset)
    }

    @Test
    fun toggleLabelsUseDedicatedEnabledAndDisabledTokens() {
        val colors = GemColors(
            themeToggleLabelInk = Color(0xFF102030),
            disabledInk = Color(0xFF405060),
        )
        val typeScale = GemTypeScale(
            themeToggleLabel = TextStyle(fontSize = 13.sp),
        )

        assertEquals(Color(0xFF102030), ThemeModeToggleInteraction.labelColor(colors, enabled = true))
        assertEquals(Color(0xFF405060), ThemeModeToggleInteraction.labelColor(colors, enabled = false))
        assertEquals(typeScale.themeToggleLabel, ThemeModeToggleInteraction.labelStyle(typeScale))
    }
}
