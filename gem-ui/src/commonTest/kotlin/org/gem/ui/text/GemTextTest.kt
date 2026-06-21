package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemTextTest {
    @Test
    fun fixedKeysCoverPrototypeCopy() {
        assertEquals(123, GemTextKey.fixedKeys.size)
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
        assertTrue(GemTextKey.GemDefault in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ThemePreferenceUnavailable in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ThemePreferenceSaveFailed in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Customise in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.Themes in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.EnterNewThemeName in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ChooseTheme in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceTextTitleBar in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceTextThemeToggleLabels in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementPageBackground in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementAccentText in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementErrorText in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementStatusText in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementMenuDisabledText in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementInteractiveHoverText in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.AppearanceElementRulesAndSeparators in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.NoAttachmentsAdded in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.ClearAttachment in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendingNotices in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.NoticesSent in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SomeNoticesFailed in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendFailureCannotSendNotices in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendFailureRejected in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendFailureSenderUnavailable in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendFailureRequestInvalid in GemTextKey.fixedKeys)
        assertTrue(GemTextKey.SendFailureSessionNotReady in GemTextKey.fixedKeys)
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
        assertEquals("GEM Default", catalogue.text(GemTextKey.GemDefault))
        assertEquals("Theme preference unavailable", catalogue.text(GemTextKey.ThemePreferenceUnavailable))
        assertEquals("Theme preference could not be saved", catalogue.text(GemTextKey.ThemePreferenceSaveFailed))
        assertEquals("Customise", catalogue.text(GemTextKey.Customise))
        assertEquals("Themes", catalogue.text(GemTextKey.Themes))
        assertEquals("Save theme", catalogue.text(GemTextKey.SaveTheme))
        assertEquals("Reset to default", catalogue.text(GemTextKey.ResetToDefault))
        assertEquals("Enter a new theme name", catalogue.text(GemTextKey.EnterNewThemeName))
        assertEquals("Save", catalogue.text(GemTextKey.Save))
        assertEquals("\u2014 choose theme \u2014", catalogue.text(GemTextKey.ChooseTheme))
        assertEquals("Text", catalogue.text(GemTextKey.Text))
        assertEquals("Fonts", catalogue.text(GemTextKey.Fonts))
        assertEquals("Element", catalogue.text(GemTextKey.Element))
        assertEquals("Accent text", catalogue.text(GemTextKey.AppearanceElementAccentText))
        assertEquals("Error text", catalogue.text(GemTextKey.AppearanceElementErrorText))
        assertEquals("Status text", catalogue.text(GemTextKey.AppearanceElementStatusText))
        assertEquals("Menu disabled text", catalogue.text(GemTextKey.AppearanceElementMenuDisabledText))
        assertEquals("Interactive hover text", catalogue.text(GemTextKey.AppearanceElementInteractiveHoverText))
        assertEquals("No attachments added", catalogue.text(GemTextKey.NoAttachmentsAdded))
        assertEquals("Clear attachment", catalogue.text(GemTextKey.ClearAttachment))
        assertEquals("Sending notices", catalogue.text(GemTextKey.SendingNotices))
        assertEquals("Notices sent", catalogue.text(GemTextKey.NoticesSent))
        assertEquals("Some notices could not be sent", catalogue.text(GemTextKey.SomeNoticesFailed))
        assertEquals(
            "Your avatar cannot send notices to this group.",
            catalogue.text(GemTextKey.SendFailureCannotSendNotices),
        )
        assertEquals(
            "Second Life did not accept the notice send.",
            catalogue.text(GemTextKey.SendFailureRejected),
        )
        assertEquals(
            "The notice sender was not available.",
            catalogue.text(GemTextKey.SendFailureSenderUnavailable),
        )
        assertEquals(
            "The notice request could not be prepared.",
            catalogue.text(GemTextKey.SendFailureRequestInvalid),
        )
        assertEquals(
            "The logged-in avatar session was not ready.",
            catalogue.text(GemTextKey.SendFailureSessionNotReady),
        )
    }

    @Test
    fun dynamicKeysRenderStatefulCopy() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("0 chars", catalogue.text(GemTextKey.DraftCharCount(0)))
        assertEquals("249 chars", catalogue.text(GemTextKey.DraftCharCount(249)))
        assertEquals("0 selected", catalogue.text(GemTextKey.SelectedCount(0)))
        assertEquals("3 selected", catalogue.text(GemTextKey.SelectedCount(3)))
        assertEquals(
            "Owks: Second Life did not accept the notice send.",
            catalogue.text(
                GemTextKey.SendFailureDetailLine(
                    groupName = "Owks",
                    reason = "Second Life did not accept the notice send.",
                ),
            ),
        )
    }
}
