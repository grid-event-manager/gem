package org.gem.ui.screens

import org.gem.ui.design.ResolvedThemeMode
import org.gem.ui.state.ThemeUiState
import org.gem.ui.testtags.GemTestTags
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
    fun themeToggleStateComesFromResolvedThemeMode() {
        assertFalse(ThemeUiState(resolvedMode = ResolvedThemeMode.LIGHT).toggleChecked)
        assertTrue(ThemeUiState(resolvedMode = ResolvedThemeMode.DARK).toggleChecked)
    }
}
