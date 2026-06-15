package org.gem.ui.components

import androidx.compose.ui.unit.dp
import org.gem.ui.design.GemSpacing
import org.gem.ui.design.GemTypeScale
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShellComponentStateTest {
    @Test
    fun shellControlsKeepPrototypeHooksAndLabels() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("data-menu-button", GemTestTags.MenuButton)
        assertEquals("data-second-life-time", GemTestTags.SecondLifeTime)
        assertEquals("data-app-menu", GemTestTags.AppMenu)
        assertEquals("data-open-settings", GemTestTags.OpenSettings)
        assertEquals("data-log-out", GemTestTags.LogOut)
        assertEquals("data-settings-back", GemTestTags.SettingsBack)
        assertEquals("data-exit", GemTestTags.Exit)
        assertEquals("data-session-status", GemTestTags.SessionStatus)
        assertEquals("data-delete-modal", GemTestTags.DeleteModal)
        assertEquals("GEM", catalogue.text(GemTextKey.BrandInitials))
        assertEquals("GRID EVENT MANAGER", catalogue.text(GemTextKey.BrandSubtitle))
        assertEquals("Menu", catalogue.text(GemTextKey.Menu))
        assertEquals("Settings", catalogue.text(GemTextKey.Settings))
        assertEquals("Log out", catalogue.text(GemTextKey.LogOut))
        assertEquals("Exit", catalogue.text(GemTextKey.Exit))
        assertEquals("BACK", catalogue.text(GemTextKey.Back))
    }

    @Test
    fun modalActionsUseSharedEqualWidthToken() {
        assertEquals(112.dp, GemSpacing().modalActionMinWidth)
    }

    @Test
    fun themeBrandAndMenuUseSharedShellTokens() {
        val spacing = GemSpacing()

        assertEquals(40.dp, spacing.brandLogoSize)
        assertEquals(72.dp, spacing.secondLifeTimeMinWidth)
        assertEquals(8.dp, spacing.secondLifeTimeMenuGap)
        assertEquals(18.dp, spacing.backIconSize)
        assertEquals(30.dp, spacing.menuItemMinHeight)
        assertEquals(12.dp, spacing.menuItemHorizontalPadding)
        assertEquals(3.dp, spacing.menuItemVerticalPadding)
        assertNull(GemTypeScale().menuItem.fontWeight)
    }
}
