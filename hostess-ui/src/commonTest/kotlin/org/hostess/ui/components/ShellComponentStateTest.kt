package org.hostess.ui.components

import androidx.compose.ui.unit.dp
import org.hostess.ui.design.HostessSpacing
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellComponentStateTest {
    @Test
    fun shellControlsKeepPrototypeHooksAndLabels() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("data-menu-button", HostessTestTags.MenuButton)
        assertEquals("data-app-menu", HostessTestTags.AppMenu)
        assertEquals("data-settings-back", HostessTestTags.SettingsBack)
        assertEquals("data-session-status", HostessTestTags.SessionStatus)
        assertEquals("data-delete-modal", HostessTestTags.DeleteModal)
        assertEquals("Menu", catalogue.text(HostessTextKey.Menu))
        assertEquals("BACK", catalogue.text(HostessTextKey.Back))
    }

    @Test
    fun modalActionsUseSharedEqualWidthToken() {
        assertEquals(112.dp, HostessSpacing().modalActionMinWidth)
    }
}
