package org.gem.ui.controllers

import org.gem.core.services.CredentialServiceRevealPasswordResult
import org.gem.core.services.SavedAccountAddResult
import org.gem.core.services.SavedAccountDeleteResult
import org.gem.ui.state.LoginEntryMode
import org.gem.ui.state.UiRoute
import org.gem.ui.testing.FakeLastLoginProfilePreferenceStore
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginControllerTest {
    @Test
    fun refreshAndSelectSavedLoginRevealPasswordMaskedByDefault() {
        val profile = FakeGemUiRuntime.defaultProfile()
        val controller = LoginController(FakeGemUiRuntime.ready())
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
        val profile = FakeGemUiRuntime.defaultProfile()
        val revealed = LoginController(FakeGemUiRuntime.ready())
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
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
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
        val runtime = FakeGemUiRuntime.ready(avatarRegionName = null)
        val profile = FakeGemUiRuntime.defaultProfile()
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
        val runtime = FakeGemUiRuntime.ready(loginSucceeds = false)
        val profile = FakeGemUiRuntime.defaultProfile()
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
        assertEquals(GemTextKey.LoginFailed, failed.state.operation.errorKey)
        assertEquals("test-password", revealedPassword(runtime, profile.profileId))
    }

    @Test
    fun avatarReadinessFailureBlocksComposeRoute() {
        val runtime = FakeGemUiRuntime.ready(avatarReady = false)
        val profile = FakeGemUiRuntime.defaultProfile()
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
        val runtime = FakeGemUiRuntime.ready()
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
        val profile = FakeGemUiRuntime.defaultProfile()
        val lastLoginStore = FakeLastLoginProfilePreferenceStore(profile.profileId)
        val runtime = FakeGemUiRuntime.ready(lastLoginProfilePreferenceStore = lastLoginStore)
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
        val profile = FakeGemUiRuntime.defaultProfile()
        val controller = LoginController(FakeGemUiRuntime.unavailable())
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
        val started = LoginController(FakeGemUiRuntime.ready())
            .refreshSavedLogins()
            .updateUsernameDraft("newhost")
            .normalizeLoginNameOnPasswordFocus()
            .updatePasswordDraft("new-password")
            .beginLogin()

        assertTrue(started.state.operation.inFlight)
        assertEquals(GemTextKey.SavingLogin, started.state.operation.messageKey)
    }

    @Test
    fun typingUsernameClearsSavedSelectionAndUsesNewEntryMode() {
        val profile = FakeGemUiRuntime.defaultProfile()
        val typed = LoginController(FakeGemUiRuntime.ready())
            .refreshSavedLogins()
            .selectSavedLogin(profile.profileId)
            .updateUsernameDraft("anotherhost")

        assertNull(typed.state.selectedProfileId)
        assertEquals(LoginEntryMode.New, typed.state.entryMode)
        assertEquals("anotherhost", typed.state.usernameDraft)
    }

    @Test
    fun refreshSavedLoginsClearsStaleDeletedProfileAndPassword() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
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
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
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
        runtime: org.gem.ui.runtime.GemUiRuntime,
        profileId: org.gem.core.domain.AccountProfileId,
    ): String {
        val result = runtime.credentialServiceOrNull()?.revealPassword(profileId)
        require(result is CredentialServiceRevealPasswordResult.Revealed)
        return result.password
    }
}
