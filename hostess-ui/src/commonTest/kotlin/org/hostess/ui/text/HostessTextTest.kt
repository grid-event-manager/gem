package org.hostess.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostessTextTest {
    @Test
    fun fixedKeysCoverPrototypeCopy() {
        assertEquals(64, HostessTextKey.fixedKeys.size)
        assertTrue(HostessTextKey.AppName in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Username in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Menu in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.LogOut in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.AddGroups in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.SavingLogin in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.SendingLoginDetails in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.RezzingWorld in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.LoadingAvatar in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.LoggingOut in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.MissingSubject in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.LoadingInventory in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.InventoryEmpty in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.InventoryUnavailable in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.GroupsEmpty in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.GroupsUnavailable in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.RemovingFailedLogin in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.BlankStatus in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Light in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.Dark in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.ThemePreferenceUnavailable in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.ThemePreferenceSaveFailed in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.NoAttachmentsAdded in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.ClearAttachment in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.SendingNotices in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.NoticesSent in HostessTextKey.fixedKeys)
        assertTrue(HostessTextKey.SomeNoticesFailed in HostessTextKey.fixedKeys)
    }

    @Test
    fun englishCatalogueProvidesPrototypeCopy() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("Ella Hostess", catalogue.text(HostessTextKey.AppName))
        assertEquals("User Name", catalogue.text(HostessTextKey.Username))
        assertEquals("Password", catalogue.text(HostessTextKey.Password))
        assertEquals("Show", catalogue.text(HostessTextKey.Show))
        assertEquals("Hide", catalogue.text(HostessTextKey.Hide))
        assertEquals("Menu", catalogue.text(HostessTextKey.Menu))
        assertEquals("Log out", catalogue.text(HostessTextKey.LogOut))
        assertEquals("\u2014 select \u2014", catalogue.text(HostessTextKey.SavedLoginPlaceholder))
        assertEquals("Add new login...", catalogue.text(HostessTextKey.AddNewLogin))
        assertEquals("Save and login", catalogue.text(HostessTextKey.SaveAndLogin))
        assertEquals("Edit account", catalogue.text(HostessTextKey.EditAccount))
        assertEquals("Save password", catalogue.text(HostessTextKey.SavePassword))
        assertEquals("Add new account", catalogue.text(HostessTextKey.AddNewAccount))
        assertEquals("Save new account", catalogue.text(HostessTextKey.SaveNewAccount))
        assertEquals("Are you sure you want to delete these accounts?", catalogue.text(HostessTextKey.DeleteConfirmation))
        assertEquals("Send notices", catalogue.text(HostessTextKey.SendNotices))
        assertEquals("Subject required", catalogue.text(HostessTextKey.MissingSubject))
        assertEquals("Body required", catalogue.text(HostessTextKey.MissingBody))
        assertEquals("Select groups", catalogue.text(HostessTextKey.MissingGroups))
        assertEquals("Logging out", catalogue.text(HostessTextKey.LoggingOut))
        assertEquals("Sending login details", catalogue.text(HostessTextKey.SendingLoginDetails))
        assertEquals("Rezzing world", catalogue.text(HostessTextKey.RezzingWorld))
        assertEquals("Loading avatar", catalogue.text(HostessTextKey.LoadingAvatar))
        assertEquals("None", catalogue.text(HostessTextKey.None))
        assertEquals("Loading inventory folders", catalogue.text(HostessTextKey.LoadingInventory))
        assertEquals("No inventory", catalogue.text(HostessTextKey.InventoryEmpty))
        assertEquals("Inventory unavailable", catalogue.text(HostessTextKey.InventoryUnavailable))
        assertEquals("Loading groups", catalogue.text(HostessTextKey.LoadingGroups))
        assertEquals("No groups", catalogue.text(HostessTextKey.GroupsEmpty))
        assertEquals("Groups unavailable", catalogue.text(HostessTextKey.GroupsUnavailable))
        assertEquals("Online", catalogue.text(HostessTextKey.Online))
        assertEquals("Offline", catalogue.text(HostessTextKey.Offline))
        assertEquals("", catalogue.text(HostessTextKey.BlankStatus))
        assertEquals("Light", catalogue.text(HostessTextKey.Light))
        assertEquals("Dark", catalogue.text(HostessTextKey.Dark))
        assertEquals("Theme preference unavailable", catalogue.text(HostessTextKey.ThemePreferenceUnavailable))
        assertEquals("Theme preference could not be saved", catalogue.text(HostessTextKey.ThemePreferenceSaveFailed))
        assertEquals("No attachments added", catalogue.text(HostessTextKey.NoAttachmentsAdded))
        assertEquals("Clear attachment", catalogue.text(HostessTextKey.ClearAttachment))
        assertEquals("Sending notices", catalogue.text(HostessTextKey.SendingNotices))
        assertEquals("Notices sent", catalogue.text(HostessTextKey.NoticesSent))
        assertEquals("Some notices failed", catalogue.text(HostessTextKey.SomeNoticesFailed))
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
