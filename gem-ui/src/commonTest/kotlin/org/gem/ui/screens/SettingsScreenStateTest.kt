package org.gem.ui.screens

import org.gem.ui.state.SettingsUiState
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsScreenStateTest {
    @Test
    fun settingsScreenUsesCanonicalTextKeys() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Settings", catalogue.text(GemTextKey.Settings))
        assertEquals("User Name", catalogue.text(GemTextKey.Username))
        assertEquals("Password", catalogue.text(GemTextKey.Password))
        assertEquals("Show", catalogue.text(GemTextKey.Show))
        assertEquals("Hide", catalogue.text(GemTextKey.Hide))
        assertEquals("Edit account", catalogue.text(GemTextKey.EditAccount))
        assertEquals("Save password", catalogue.text(GemTextKey.SavePassword))
        assertEquals("Add new account", catalogue.text(GemTextKey.AddNewAccount))
        assertEquals("Save new account", catalogue.text(GemTextKey.SaveNewAccount))
        assertEquals("Delete account", catalogue.text(GemTextKey.DeleteAccount))
        assertEquals("Delete", catalogue.text(GemTextKey.Delete))
        assertEquals("OK", catalogue.text(GemTextKey.Ok))
        assertEquals("Cancel", catalogue.text(GemTextKey.Cancel))
        assertEquals("BACK", catalogue.text(GemTextKey.Back))
        assertEquals("Light", catalogue.text(GemTextKey.Light))
        assertEquals("Dark", catalogue.text(GemTextKey.Dark))
    }

    @Test
    fun settingsScreenStartsCollapsedAndDisabled() {
        val state = SettingsUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)

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
        assertEquals("data-view-settings", GemTestTags.ViewSettings)
        assertEquals("data-settings-back", GemTestTags.SettingsBack)
        assertEquals("data-account-name", GemTestTags.AccountName)
        assertEquals("data-account-password", GemTestTags.AccountPassword)
        assertEquals("data-delete-account-panel", GemTestTags.DeleteAccountPanel)
        assertEquals("data-delete-account-list", GemTestTags.DeleteAccountList)
        assertEquals("data-delete-account", GemTestTags.DeleteAccount)
        assertEquals("data-delete-modal", GemTestTags.DeleteModal)
        assertEquals("data-confirm-delete", GemTestTags.ConfirmDelete)
        assertEquals("data-cancel-delete", GemTestTags.CancelDelete)
    }
}
