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
        assertEquals("data-settings-back", GemTestTags.SettingsBack)
        assertEquals("data-session-status", GemTestTags.SessionStatus)
        assertEquals("data-delete-modal", GemTestTags.DeleteModal)
        assertEquals("Menu", catalogue.text(GemTextKey.Menu))
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
        assertEquals(18.dp, spacing.backIconSize)
        assertEquals(30.dp, spacing.menuItemMinHeight)
        assertEquals(12.dp, spacing.menuItemHorizontalPadding)
        assertEquals(3.dp, spacing.menuItemVerticalPadding)
        assertNull(GemTypeScale().menuItem.fontWeight)
    }
}
