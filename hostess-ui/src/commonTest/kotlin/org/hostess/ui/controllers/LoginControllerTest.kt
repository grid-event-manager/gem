package org.hostess.ui.controllers

import org.hostess.core.services.CredentialServiceRevealPasswordResult
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeHostessUiRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginControllerTest {
    @Test
    fun refreshAndSelectSavedLoginRevealPasswordMaskedByDefault() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val controller = LoginController(FakeHostessUiRuntime.ready())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)

        assertEquals(listOf(profile.profileId), controller.state.savedLoginOptions.map { it.profileId })
        assertEquals(profile.profileId, controller.state.selectedProfileId)
        assertEquals("test-password", controller.state.passwordDraft)
        assertFalse(controller.state.passwordVisible)
        assertTrue(controller.state.passwordEnabled)
        assertTrue(controller.state.loginEnabled)
    }

    @Test
    fun hideMasksPasswordWithoutBlankingDraft() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val revealed = LoginController(FakeHostessUiRuntime.ready())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .toggleSavedPasswordVisibility()
        val hidden = revealed.toggleSavedPasswordVisibility()

        assertTrue(revealed.state.passwordVisible)
        assertFalse(hidden.state.passwordVisible)
        assertEquals("test-password", hidden.state.passwordDraft)
    }

    @Test
    fun successfulSavedLoginPersistsEditedPasswordAndRoutesAfterReadiness() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val loggedIn = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateSavedPasswordDraft("changed-password")
            .loginSelected()

        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals(profile.loginName.value, loggedIn.appState.activeAccountLabel)
        assertNull(loggedIn.state.selectedProfileId)
        assertEquals("", loggedIn.state.passwordDraft)
        assertEquals("changed-password", revealedPassword(runtime, profile.profileId))
    }

    @Test
    fun failedSavedLoginPreservesDraftAndRestoresSavedCredential() {
        val runtime = FakeHostessUiRuntime.ready(loginSucceeds = false)
        val profile = FakeHostessUiRuntime.defaultProfile()
        val failed = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateSavedPasswordDraft("changed-password")
            .loginSelected()

        assertEquals(UiRoute.Login, failed.appState.route)
        assertEquals(profile.profileId, failed.state.selectedProfileId)
        assertEquals("changed-password", failed.state.passwordDraft)
        assertEquals("test-password", revealedPassword(runtime, profile.profileId))
    }

    @Test
    fun avatarReadinessFailureBlocksComposeRoute() {
        val runtime = FakeHostessUiRuntime.ready(avatarReady = false)
        val profile = FakeHostessUiRuntime.defaultProfile()
        val failed = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .loginSelected()

        assertEquals(UiRoute.Login, failed.appState.route)
        assertNull(failed.appState.session)
        assertEquals("test-password", failed.state.passwordDraft)
    }

    @Test
    fun newLoginNameNormalizesOnPasswordFocusAndSaveAndLoginRoutesAfterReadiness() {
        val runtime = FakeHostessUiRuntime.ready()
        val loggedIn = LoginController(runtime)
            .refreshSavedLogins()
            .toggleAddLoginPanel()
            .updateNewUsernameDraft("newhost")
            .normalizeNewLoginNameOnPasswordFocus()
            .updateNewPasswordDraft("new-password")
            .saveAndLogin()

        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals("newhost resident", loggedIn.appState.activeAccountLabel)
        assertEquals("", loggedIn.state.newUsernameDraft)
        assertEquals("", loggedIn.state.newPasswordDraft)
        assertFalse(loggedIn.state.newPasswordVisible)
    }

    @Test
    fun unavailableCredentialRuntimeDisablesLoginPaths() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val controller = LoginController(FakeHostessUiRuntime.unavailable())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateNewUsernameDraft("newhost")
            .updateNewPasswordDraft("new-password")
            .saveAndLogin()

        assertFalse(controller.state.passwordEnabled)
        assertFalse(controller.state.loginEnabled)
        assertFalse(controller.state.saveAndLoginEnabled)
    }

    private fun revealedPassword(
        runtime: org.hostess.ui.runtime.HostessUiRuntime,
        profileId: org.hostess.core.domain.AccountProfileId,
    ): String {
        val result = runtime.credentialServiceOrNull()?.revealPassword(profileId)
        require(result is CredentialServiceRevealPasswordResult.Revealed)
        return result.password
    }
}
