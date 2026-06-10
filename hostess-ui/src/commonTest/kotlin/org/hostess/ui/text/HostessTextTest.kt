package org.hostess.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostessTextTest {
    @Test
    fun fixedKeysCoverPrototypeCopy() {
        assertEquals(42, HostessTextKey.fixedKeys.size)
        assertTrue(HostessTextKey.AppName in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Username in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Menu in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.LogOut in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.AddGroups in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.BlankStatus in HostessTextKey.fixedKeys)
    }

    @Test
    fun englishCatalogueProvidesPrototypeCopy() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("Hostess", catalogue.text(HostessTextKey.AppName))
        assertEquals("User Name", catalogue.text(HostessTextKey.Username))
        assertEquals("Password", catalogue.text(HostessTextKey.Password))
        assertEquals("Show", catalogue.text(HostessTextKey.Show))
        assertEquals("Hide", catalogue.text(HostessTextKey.Hide))
        assertEquals("Menu", catalogue.text(HostessTextKey.Menu))
        assertEquals("Log out", catalogue.text(HostessTextKey.LogOut))
        assertEquals("\u2014 select \u2014", catalogue.text(HostessTextKey.SavedLoginPlaceholder))
        assertEquals("Add new login...", catalogue.text(HostessTextKey.AddNewLogin))
        assertEquals("Save and login", catalogue.text(HostessTextKey.SaveAndLogin))
        assertEquals("Add new account", catalogue.text(HostessTextKey.AddNewAccount))
        assertEquals("Save new account", catalogue.text(HostessTextKey.SaveNewAccount))
        assertEquals("Are you sure you want to delete these accounts?", catalogue.text(HostessTextKey.DeleteConfirmation))
        assertEquals("Send notices", catalogue.text(HostessTextKey.SendNotices))
        assertEquals("Online", catalogue.text(HostessTextKey.Online))
        assertEquals("Offline", catalogue.text(HostessTextKey.Offline))
        assertEquals("", catalogue.text(HostessTextKey.BlankStatus))
    }

    @Test
    fun dynamicKeysRenderStatefulCopy() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("0 chars", catalogue.text(HostessTextKey.DraftCharCount(0)))
        assertEquals("249 chars", catalogue.text(HostessTextKey.DraftCharCount(249)))
        assertEquals("0 selected", catalogue.text(HostessTextKey.SelectedCount(0)))
        assertEquals("3 selected", catalogue.text(HostessTextKey.SelectedCount(3)))
    }
}
