package org.hostess.ui.screens

import org.hostess.ui.state.LoginUiState
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginScreenStateTest {
    @Test
    fun loginScreenUsesCanonicalTextKeys() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("User Name", catalogue.text(HostessTextKey.Username))
        assertEquals("Password", catalogue.text(HostessTextKey.Password))
        assertEquals("Show", catalogue.text(HostessTextKey.Show))
        assertEquals("Hide", catalogue.text(HostessTextKey.Hide))
        assertEquals("Login", catalogue.text(HostessTextKey.Login))
        assertEquals("Add new login...", catalogue.text(HostessTextKey.AddNewLogin))
        assertEquals("Save and login", catalogue.text(HostessTextKey.SaveAndLogin))
    }

    @Test
    fun loginScreenStartsWithPasswordAndActionsDisabled() {
        val state = LoginUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)

        assertFalse(state.passwordEnabled)
        assertFalse(state.loginEnabled)
        assertFalse(state.addLoginExpanded)
        assertFalse(state.saveAndLoginEnabled)
    }

    @Test
    fun addLoginEnablementRequiresBothDraftFieldsAndReadyRuntime() {
        val ready = LoginUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)
        val unavailable = LoginUiState.fromCredentialRuntime(FakeHostessUiRuntime.unavailable().credentialRuntimeState)

        assertTrue(ready.credentialRuntime.ready)
        assertFalse(unavailable.credentialRuntime.ready)
    }

    @Test
    fun loginScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-login", HostessTestTags.ViewLogin)
        assertEquals("data-account-name", HostessTestTags.AccountName)
        assertEquals("data-account-password", HostessTestTags.AccountPassword)
        assertEquals("data-toggle-password", HostessTestTags.TogglePassword)
        assertEquals("data-login-button", HostessTestTags.LoginButton)
        assertEquals("data-add-account", HostessTestTags.AddAccount)
    }
}
