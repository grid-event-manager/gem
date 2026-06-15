package org.gem.ui.screens

import org.gem.ui.state.AccountsUiState
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AccountsScreenStateTest {
    @Test
    fun accountsScreenUsesCanonicalTextKeys() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Accounts", catalogue.text(GemTextKey.Accounts))
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
    }

    @Test
    fun accountsScreenStartsCollapsedAndDisabled() {
        val state = AccountsUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)

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
    fun accountsScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-accounts", GemTestTags.ViewAccounts)
        assertEquals("data-accounts-back", GemTestTags.AccountsBack)
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
