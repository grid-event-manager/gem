package org.gem.ui.screens

import org.gem.core.appearance.AppearanceMode
import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.testing.controllerBackedAppearanceState
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenStateTest {
    @Test
    fun settingsScreenUsesThemeCopyAndItsOwnViewTag() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Settings", catalogue.text(GemTextKey.Settings))
        assertEquals("Light", catalogue.text(GemTextKey.Light))
        assertEquals("Dark", catalogue.text(GemTextKey.Dark))
        assertEquals("data-view-settings", GemTestTags.ViewSettings)
    }

    @Test
    fun appearanceStateSuppliesCollapsedSettingsDefaultsAndThemeToggleState() {
        val light = controllerBackedAppearanceState(osDark = false)
        val dark = controllerBackedAppearanceState(osDark = true)

        assertEquals(AppearanceMode.LIGHT, light.mode)
        assertEquals(AppearanceExpandedPanel.NONE, light.expandedPanel)
        assertFalse(light.toggleChecked)
        assertEquals(AppearanceMode.DARK, dark.mode)
        assertEquals(AppearanceExpandedPanel.NONE, dark.expandedPanel)
        assertTrue(dark.toggleChecked)
    }
}
