package org.gem.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemTextPromptModalTest {
    @Test
    fun saveIsDisabledForBlankNames() {
        val placeholder = "Enter a new theme name"

        assertFalse(GemTextPromptModalInteraction.canSave("", placeholder))
        assertFalse(GemTextPromptModalInteraction.canSave("   ", placeholder))
        assertFalse(GemTextPromptModalInteraction.canSave(placeholder, placeholder))
        assertTrue(GemTextPromptModalInteraction.canSave("Princess Evening", placeholder))
    }

    @Test
    fun modalOrderMatchesPrototype() {
        assertEquals(listOf("theme-toggle", "name-field", "actions"), GemTextPromptModalInteraction.contentOrder)
        assertEquals(listOf("cancel", "save"), GemTextPromptModalInteraction.actionOrder)
    }

    @Test
    fun placeholderInteractionMatchesPrototype() {
        val placeholder = "Enter a new theme name"

        assertEquals(placeholder, GemTextPromptModalInteraction.displayedPlaceholder(placeholder, visible = true))
        assertNull(GemTextPromptModalInteraction.displayedPlaceholder(placeholder, visible = false))
        assertFalse(GemTextPromptModalInteraction.placeholderVisibleAfterFocusChange(focused = true, value = ""))
        assertTrue(GemTextPromptModalInteraction.placeholderVisibleAfterFocusChange(focused = false, value = ""))
        assertFalse(GemTextPromptModalInteraction.placeholderVisibleAfterFocusChange(focused = false, value = "Goth Night"))
    }
}
