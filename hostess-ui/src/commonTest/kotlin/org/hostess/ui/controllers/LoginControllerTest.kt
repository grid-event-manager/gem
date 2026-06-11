package org.hostess.ui.controllers

import org.hostess.core.services.CredentialServiceRevealPasswordResult
import org.hostess.core.services.SavedAccountAddResult
import org.hostess.core.services.SavedAccountDeleteResult
import org.hostess.ui.state.LoginEntryMode
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeLastLoginProfilePreferenceStore
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.text.HostessTextKey
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
        assertEquals(profile.loginName.value, controller.state.usernameDraft)
        assertEquals(profile.profileId, controller.state.selectedProfileId)
        assertEquals(LoginEntryMode.Saved(profile.profileId), controller.state.entryMode)
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
            .togglePasswordVisibility()
        val hidden = revealed.togglePasswordVisibility()

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
            .updatePasswordDraft("changed-password")
            .beginLogin()
            .completeLogin()

        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals(profile.loginName.value, loggedIn.appState.activeAccountLabel)
        assertEquals("London City", loggedIn.appState.sessionStrip.locationLabel)
        assertEquals(profile.profileId, loggedIn.state.selectedProfileId)
        assertEquals("changed-password", loggedIn.state.passwordDraft)
        assertEquals("changed-password", revealedPassword(runtime, profile.profileId))
    }

    @Test
    fun successfulLoginUsesBlankLocationWhenReadinessProofHasNoRegionName() {
        val runtime = FakeHostessUiRuntime.ready(avatarRegionName = null)
        val profile = FakeHostessUiRuntime.defaultProfile()
        val loggedIn = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .beginLogin()
            .completeLogin()

        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals("", loggedIn.appState.sessionStrip.locationLabel)
    }

    @Test
    fun failedSavedLoginPreservesDraftAndRestoresSavedCredential() {
        val runtime = FakeHostessUiRuntime.ready(loginSucceeds = false)
        val profile = FakeHostessUiRuntime.defaultProfile()
        val failed = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updatePasswordDraft("changed-password")
            .beginLogin()
            .completeLogin()

        assertEquals(UiRoute.Login, failed.appState.route)
        assertEquals(profile.profileId, failed.state.selectedProfileId)
        assertEquals("changed-password", failed.state.passwordDraft)
        assertTrue(failed.state.passwordEnabled)
        assertTrue(failed.state.loginEnabled)
        assertEquals(HostessTextKey.LoginFailed, failed.state.operation.errorKey)
        assertEquals("test-password", revealedPassword(runtime, profile.profileId))
    }

    @Test
    fun avatarReadinessFailureBlocksComposeRoute() {
        val runtime = FakeHostessUiRuntime.ready(avatarReady = false)
        val profile = FakeHostessUiRuntime.defaultProfile()
        val failed = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .beginLogin()
            .completeLogin()

        assertEquals(UiRoute.Login, failed.appState.route)
        assertNull(failed.appState.session)
        assertEquals("test-password", failed.state.passwordDraft)
    }

    @Test
    fun newLoginNameNormalizesOnPasswordFocusAndSaveAndLoginRoutesAfterReadiness() {
        val runtime = FakeHostessUiRuntime.ready()
        val loggedIn = LoginController(runtime)
            .refreshSavedLogins()
            .updateUsernameDraft("newhost")
            .normalizeLoginNameOnPasswordFocus()
            .updatePasswordDraft("new-password")
            .beginLogin()
            .completeLogin()

        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals("newhost resident", loggedIn.appState.activeAccountLabel)
        assertEquals("newhost resident", loggedIn.state.usernameDraft)
        assertEquals("new-password", loggedIn.state.passwordDraft)
        assertFalse(loggedIn.state.passwordVisible)
    }

    @Test
    fun refreshSavedLoginsRehydratesLastUsedProfileAndSuccessfulLoginSavesIt() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val lastLoginStore = FakeLastLoginProfilePreferenceStore(profile.profileId)
        val runtime = FakeHostessUiRuntime.ready(lastLoginProfilePreferenceStore = lastLoginStore)
        val refreshed = LoginController(runtime).refreshSavedLogins()
        val loggedIn = refreshed.beginLogin().completeLogin()

        assertEquals(profile.profileId, refreshed.state.selectedProfileId)
        assertEquals(profile.loginName.value, refreshed.state.usernameDraft)
        assertEquals("test-password", refreshed.state.passwordDraft)
        assertEquals(listOf(profile.profileId), lastLoginStore.savedProfileIds)
        assertEquals(UiRoute.Compose, loggedIn.appState.route)
    }

    @Test
    fun unavailableCredentialRuntimeDisablesLoginPaths() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val controller = LoginController(FakeHostessUiRuntime.unavailable())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateUsernameDraft("newhost")
            .updatePasswordDraft("new-password")
            .beginLogin()

        assertFalse(controller.state.passwordEnabled)
        assertFalse(controller.state.loginEnabled)
        assertFalse(controller.state.operation.inFlight)
    }

    @Test
    fun beginLoginSetsVisibleProgressBeforeWorkflowCompletes() {
        val started = LoginController(FakeHostessUiRuntime.ready())
            .refreshSavedLogins()
            .updateUsernameDraft("newhost")
            .normalizeLoginNameOnPasswordFocus()
            .updatePasswordDraft("new-password")
            .beginLogin()

        assertTrue(started.state.operation.inFlight)
        assertEquals(HostessTextKey.SavingLogin, started.state.operation.messageKey)
    }

    @Test
    fun typingUsernameClearsSavedSelectionAndUsesNewEntryMode() {
        val profile = FakeHostessUiRuntime.defaultProfile()
        val typed = LoginController(FakeHostessUiRuntime.ready())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateUsernameDraft("anotherhost")

        assertNull(typed.state.selectedProfileId)
        assertEquals(LoginEntryMode.New, typed.state.entryMode)
        assertEquals("anotherhost", typed.state.usernameDraft)
    }

    @Test
    fun refreshSavedLoginsClearsStaleDeletedProfileAndPassword() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val stale = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
        val deleted = runtime.savedAccountManagementServiceOrNull()
            ?.deleteAccounts(setOf(profile.profileId))

        val refreshed = stale.refreshSavedLogins()

        assertEquals(SavedAccountDeleteResult.Deleted(setOf(profile.profileId)), deleted)
        assertEquals(emptyList(), refreshed.state.savedLoginOptions)
        assertEquals("", refreshed.state.usernameDraft)
        assertEquals("", refreshed.state.passwordDraft)
        assertNull(refreshed.state.selectedProfileId)
        assertEquals(LoginEntryMode.New, refreshed.state.entryMode)
        assertFalse(refreshed.state.passwordEnabled)
        assertFalse(refreshed.state.loginEnabled)
    }

    @Test
    fun refreshSavedLoginsRehydratesReaddedProfilePasswordAfterDelete() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val stale = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
        val accountService = runtime.savedAccountManagementServiceOrNull() ?: error("missing account service")

        accountService.deleteAccounts(setOf(profile.profileId))
        val added = accountService.addAccount("venuehost", "replacement-password")
        require(added is SavedAccountAddResult.Saved)

        val refreshed = stale.refreshSavedLogins()

        assertEquals(added.profile.profileId, refreshed.state.selectedProfileId)
        assertEquals("venuehost resident", refreshed.state.usernameDraft)
        assertEquals("replacement-password", refreshed.state.passwordDraft)
        assertTrue(refreshed.state.passwordEnabled)
        assertTrue(refreshed.state.loginEnabled)
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
