package org.gem.ui.screens

import org.gem.ui.state.LoginUiState
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginScreenStateTest {
    @Test
    fun loginScreenUsesCanonicalTextKeys() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("User Name", catalogue.text(GemTextKey.Username))
        assertEquals("Password", catalogue.text(GemTextKey.Password))
        assertEquals("Show", catalogue.text(GemTextKey.Show))
        assertEquals("Hide", catalogue.text(GemTextKey.Hide))
        assertEquals("Login", catalogue.text(GemTextKey.Login))
        assertEquals("Saving login", catalogue.text(GemTextKey.SavingLogin))
        assertEquals("Sending login details", catalogue.text(GemTextKey.SendingLoginDetails))
        assertEquals("Preparing avatar", catalogue.text(GemTextKey.PreparingAvatar))
        assertEquals("Loading groups", catalogue.text(GemTextKey.LoadingGroups))
        assertEquals("Loading inventory folders", catalogue.text(GemTextKey.LoadingInventory))
        assertEquals("Login failed", catalogue.text(GemTextKey.LoginFailed))
        assertEquals("Removing failed login", catalogue.text(GemTextKey.RemovingFailedLogin))
    }

    @Test
    fun loginScreenStartsWithPasswordAndActionsDisabled() {
        val state = LoginUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)

        assertFalse(state.passwordEnabled)
        assertFalse(state.loginEnabled)
        assertFalse(state.operation.inFlight)
        assertEquals("", state.usernameDraft)
    }

    @Test
    fun loginEnablementRequiresReadyRuntime() {
        val ready = LoginUiState.fromCredentialRuntime(FakeGemUiRuntime.ready().credentialRuntimeState)
        val unavailable = LoginUiState.fromCredentialRuntime(FakeGemUiRuntime.unavailable().credentialRuntimeState)

        assertTrue(ready.credentialRuntime.ready)
        assertFalse(unavailable.credentialRuntime.ready)
    }

    @Test
    fun loginScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-login", GemTestTags.ViewLogin)
        assertEquals("data-account-name", GemTestTags.AccountName)
        assertEquals("data-account-password", GemTestTags.AccountPassword)
        assertEquals("data-toggle-password", GemTestTags.TogglePassword)
        assertEquals("data-login-button", GemTestTags.LoginButton)
    }
}
