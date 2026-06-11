package org.hostess.ui.screens

import org.hostess.ui.state.SettingsUiState
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsScreenStateTest {
    @Test
    fun settingsScreenUsesCanonicalTextKeys() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("Settings", catalogue.text(HostessTextKey.Settings))
        assertEquals("User Name", catalogue.text(HostessTextKey.Username))
        assertEquals("Password", catalogue.text(HostessTextKey.Password))
        assertEquals("Show", catalogue.text(HostessTextKey.Show))
        assertEquals("Hide", catalogue.text(HostessTextKey.Hide))
        assertEquals("Edit account", catalogue.text(HostessTextKey.EditAccount))
        assertEquals("Save password", catalogue.text(HostessTextKey.SavePassword))
        assertEquals("Add new account", catalogue.text(HostessTextKey.AddNewAccount))
        assertEquals("Save new account", catalogue.text(HostessTextKey.SaveNewAccount))
        assertEquals("Delete account", catalogue.text(HostessTextKey.DeleteAccount))
        assertEquals("Delete", catalogue.text(HostessTextKey.Delete))
        assertEquals("OK", catalogue.text(HostessTextKey.Ok))
        assertEquals("Cancel", catalogue.text(HostessTextKey.Cancel))
        assertEquals("BACK", catalogue.text(HostessTextKey.Back))
        assertEquals("Light", catalogue.text(HostessTextKey.Light))
        assertEquals("Dark", catalogue.text(HostessTextKey.Dark))
    }

    @Test
    fun settingsScreenStartsCollapsedAndDisabled() {
        val state = SettingsUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)

        assertFalse(state.passwordEnabled)
        assertFalse(state.passwordVisible)
        assertFalse(state.editAccountExpanded)
        assertFalse(state.addAccountExpanded)
        assertFalse(state.saveNewAccountEnabled)
        assertFalse(state.deleteExpanded)
        assertFalse(state.deleteEnabled)
        assertFalse(state.confirmDeleteOpen)
    }

    @Test
    fun settingsScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-settings", HostessTestTags.ViewSettings)
        assertEquals("data-settings-back", HostessTestTags.SettingsBack)
        assertEquals("data-account-name", HostessTestTags.AccountName)
        assertEquals("data-account-password", HostessTestTags.AccountPassword)
        assertEquals("data-delete-account-panel", HostessTestTags.DeleteAccountPanel)
        assertEquals("data-delete-account-list", HostessTestTags.DeleteAccountList)
        assertEquals("data-delete-account", HostessTestTags.DeleteAccount)
        assertEquals("data-delete-modal", HostessTestTags.DeleteModal)
        assertEquals("data-confirm-delete", HostessTestTags.ConfirmDelete)
        assertEquals("data-cancel-delete", HostessTestTags.CancelDelete)
    }
}
