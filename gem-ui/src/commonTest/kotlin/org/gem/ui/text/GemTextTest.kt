package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemTextTest {
    @Test
    fun fixedKeysCoverPrototypeCopy() {
        assertEquals(71, GemTextKey.fixedKeys.size)
        assertTrue(GemTextKey.AppName in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.BrandInitials in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.BrandSubtitle in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Username in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SecondLifeTimePrefix in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.MeridiemAm in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.MeridiemPm in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Menu in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.LogOut in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Exit in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Accounts in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AddGroups in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SavingLogin in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendingLoginDetails in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.RezzingWorld in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.LoadingAvatar in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.LoggingOut in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.MissingSubject in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.LoadingInventory in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.InventoryEmpty in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.InventoryUnavailable in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.GroupsEmpty in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.GroupsUnavailable in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.RemovingFailedLogin in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.BlankStatus in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Light in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Dark in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ThemePreferenceUnavailable in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ThemePreferenceSaveFailed in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.NoAttachmentsAdded in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ClearAttachment in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendingNotices in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.NoticesSent in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SomeNoticesFailed in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SomeNoticesUnconfirmed in GemTextKey.fixedKeys)
    }

    @Test
    fun englishCatalogueProvidesPrototypeCopy() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Grid Event Manager", catalogue.text(GemTextKey.AppName))
        assertEquals("GEM", catalogue.text(GemTextKey.BrandInitials))
        assertEquals("GRID EVENT MANAGER", catalogue.text(GemTextKey.BrandSubtitle))
        assertEquals("User Name", catalogue.text(GemTextKey.Username))
        assertEquals("Password", catalogue.text(GemTextKey.Password))
        assertEquals("Show", catalogue.text(GemTextKey.Show))
        assertEquals("Hide", catalogue.text(GemTextKey.Hide))
        assertEquals("SLT", catalogue.text(GemTextKey.SecondLifeTimePrefix))
        assertEquals("AM", catalogue.text(GemTextKey.MeridiemAm))
        assertEquals("PM", catalogue.text(GemTextKey.MeridiemPm))
        assertEquals("Menu", catalogue.text(GemTextKey.Menu))
        assertEquals("Accounts", catalogue.text(GemTextKey.Accounts))
        assertEquals("Log out", catalogue.text(GemTextKey.LogOut))
        assertEquals("Exit", catalogue.text(GemTextKey.Exit))
        assertEquals("\u2014 select \u2014", catalogue.text(GemTextKey.SavedLoginPlaceholder))
        assertEquals("Add new login...", catalogue.text(GemTextKey.AddNewLogin))
        assertEquals("Save and login", catalogue.text(GemTextKey.SaveAndLogin))
        assertEquals("Edit account", catalogue.text(GemTextKey.EditAccount))
        assertEquals("Save password", catalogue.text(GemTextKey.SavePassword))
        assertEquals("Add new account", catalogue.text(GemTextKey.AddNewAccount))
        assertEquals("Save new account", catalogue.text(GemTextKey.SaveNewAccount))
        assertEquals("Are you sure you want to delete these accounts?", catalogue.text(GemTextKey.DeleteConfirmation))
        assertEquals("Send notices", catalogue.text(GemTextKey.SendNotices))
        assertEquals("Subject required", catalogue.text(GemTextKey.MissingSubject))
        assertEquals("Body required", catalogue.text(GemTextKey.MissingBody))
        assertEquals("Select groups", catalogue.text(GemTextKey.MissingGroups))
        assertEquals("Logging out", catalogue.text(GemTextKey.LoggingOut))
        assertEquals("Sending login details", catalogue.text(GemTextKey.SendingLoginDetails))
        assertEquals("Rezzing world", catalogue.text(GemTextKey.RezzingWorld))
        assertEquals("Loading avatar", catalogue.text(GemTextKey.LoadingAvatar))
        assertEquals("None", catalogue.text(GemTextKey.None))
        assertEquals("Loading inventory folders", catalogue.text(GemTextKey.LoadingInventory))
        assertEquals("No inventory", catalogue.text(GemTextKey.InventoryEmpty))
        assertEquals("Inventory unavailable", catalogue.text(GemTextKey.InventoryUnavailable))
        assertEquals("Loading groups", catalogue.text(GemTextKey.LoadingGroups))
        assertEquals("No groups", catalogue.text(GemTextKey.GroupsEmpty))
        assertEquals("Groups unavailable", catalogue.text(GemTextKey.GroupsUnavailable))
        assertEquals("Online", catalogue.text(GemTextKey.Online))
        assertEquals("Offline", catalogue.text(GemTextKey.Offline))
        assertEquals("", catalogue.text(GemTextKey.BlankStatus))
        assertEquals("Light", catalogue.text(GemTextKey.Light))
        assertEquals("Dark", catalogue.text(GemTextKey.Dark))
        assertEquals("Theme preference unavailable", catalogue.text(GemTextKey.ThemePreferenceUnavailable))
        assertEquals("Theme preference could not be saved", catalogue.text(GemTextKey.ThemePreferenceSaveFailed))
        assertEquals("No attachments added", catalogue.text(GemTextKey.NoAttachmentsAdded))
        assertEquals("Clear attachment", catalogue.text(GemTextKey.ClearAttachment))
        assertEquals("Sending notices", catalogue.text(GemTextKey.SendingNotices))
        assertEquals("Notices sent", catalogue.text(GemTextKey.NoticesSent))
        assertEquals("Some notices failed", catalogue.text(GemTextKey.SomeNoticesFailed))
        assertEquals("Some notices unconfirmed", catalogue.text(GemTextKey.SomeNoticesUnconfirmed))
    }

    @Test
    fun dynamicKeysRenderStatefulCopy() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("0 chars", catalogue.text(GemTextKey.DraftCharCount(0)))
        assertEquals("249 chars", catalogue.text(GemTextKey.DraftCharCount(249)))
        assertEquals("0 selected", catalogue.text(GemTextKey.SelectedCount(0)))
        assertEquals("3 selected", catalogue.text(GemTextKey.SelectedCount(3)))
    }
}
