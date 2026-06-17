package org.gem.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals("", GemTextPromptModalInteraction.focusedValue("Enter a new theme name", "Enter a new theme name"))
        assertEquals("Enter a new theme name", GemTextPromptModalInteraction.blurredValue("", "Enter a new theme name"))
        assertEquals("Goth Night", GemTextPromptModalInteraction.blurredValue("Goth Night", "Enter a new theme name"))
    }
}
