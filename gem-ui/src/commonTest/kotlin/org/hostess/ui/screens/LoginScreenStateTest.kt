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
        assertEquals("Saving login", catalogue.text(HostessTextKey.SavingLogin))
        assertEquals("Sending login details", catalogue.text(HostessTextKey.SendingLoginDetails))
        assertEquals("Preparing avatar", catalogue.text(HostessTextKey.PreparingAvatar))
        assertEquals("Loading groups", catalogue.text(HostessTextKey.LoadingGroups))
        assertEquals("Loading inventory folders", catalogue.text(HostessTextKey.LoadingInventory))
        assertEquals("Login failed", catalogue.text(HostessTextKey.LoginFailed))
        assertEquals("Removing failed login", catalogue.text(HostessTextKey.RemovingFailedLogin))
    }

    @Test
    fun loginScreenStartsWithPasswordAndActionsDisabled() {
        val state = LoginUiState.fromCredentialRuntime(FakeHostessUiRuntime.ready().credentialRuntimeState)

        assertFalse(state.passwordEnabled)
        assertFalse(state.loginEnabled)
        assertFalse(state.operation.inFlight)
        assertEquals("", state.usernameDraft)
    }

    @Test
    fun loginEnablementRequiresReadyRuntime() {
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
    }
}
