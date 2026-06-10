package org.hostess.ui.components

import androidx.compose.ui.unit.dp
import org.hostess.ui.design.HostessSpacing
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
        val spacing = HostessSpacing()

        assertEquals(36.dp, spacing.themeToggleWidth)
        assertEquals(20.dp, spacing.themeToggleHeight)
        assertEquals(14.dp, spacing.themeToggleKnobSize)
        assertEquals(3.dp, spacing.themeToggleKnobInset)
        assertEquals(16.dp, spacing.themeToggleKnobCheckedOffset)
    }
}
