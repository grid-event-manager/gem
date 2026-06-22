package org.gem.ui.controllers

import org.gem.ui.state.AppUiState
import org.gem.ui.state.UiRoute
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeLastLoginProfilePreferenceStore
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountsControllerTest {
    @Test
    fun selectingSavedAccountRevealsMaskedPasswordAndSavesEditsExplicitly() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
        val selected = AccountsController(runtime)
            .refreshSavedAccounts()
            .selectSavedAccount(profile.profileId)

        assertEquals(profile.profileId, selected.state.selectedProfileId)
        assertEquals("test-password", selected.state.passwordDraft)
        assertTrue(selected.state.passwordEnabled)
        assertFalse(selected.state.passwordVisible)

        val updated = selected.updateSavedPasswordDraft("changed-password")
        val beforeSave = updated.selectSavedAccount(profile.profileId)
        val saved = updated.saveEditedPassword()
        val revealedAgain = saved.selectSavedAccount(profile.profileId)

        assertEquals("test-password", beforeSave.state.passwordDraft)
        assertFalse(updated.state.editAccountExpanded)
        assertEquals("changed-password", revealedAgain.state.passwordDraft)
        assertNull(revealedAgain.state.errorKey)
    }

    @Test
    fun refreshSavedAccountsSelectsLastUsedProfile() {
        val profile = FakeGemUiRuntime.defaultProfile()
        val runtime = FakeGemUiRuntime.ready(
            lastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(profile.profileId),
        )
        val refreshed = AccountsController(runtime).refreshSavedAccounts()

        assertEquals(profile.profileId, refreshed.state.selectedProfileId)
        assertEquals("test-password", refreshed.state.passwordDraft)
        assertTrue(refreshed.state.passwordEnabled)
    }

    @Test
    fun accountsWithNoSavedAccountsExpandsAddAccountAndHidesDeleteState() {
        val refreshed = AccountsController(FakeGemUiRuntime.ready(profiles = emptyList()))
            .refreshSavedAccounts()

        assertTrue(refreshed.state.savedLoginOptions.isEmpty())
        assertTrue(refreshed.state.addAccountExpanded)
        assertFalse(refreshed.state.editAccountExpanded)
        assertFalse(refreshed.state.deleteExpanded)
        assertFalse(refreshed.state.deleteEnabled)
    }

    @Test
    fun placeholderSelectionClearsAndDisablesSavedPassword() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
        val cleared = AccountsController(runtime)
            .refreshSavedAccounts()
            .selectSavedAccount(profile.profileId)
            .selectSavedAccount(null)

        assertNull(cleared.state.selectedProfileId)
        assertEquals("", cleared.state.passwordDraft)
        assertFalse(cleared.state.passwordEnabled)
        assertFalse(cleared.state.passwordVisible)
    }

    @Test
    fun addAccountNormalizesThroughCoreAndDoesNotLogIn() {
        val runtime = FakeGemUiRuntime.ready()
        val saved = AccountsController(
            runtime = runtime,
            appState = AppUiState(route = UiRoute.Accounts),
        )
            .refreshSavedAccounts()
            .toggleAddAccountPanel()
            .updateNewUsernameDraft("venuehost")
            .normalizeNewAccountNameOnPasswordFocus()
            .openDeleteAccounts()
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertEquals(UiRoute.Accounts, saved.appState.route)
        assertTrue(saved.state.savedLoginOptions.any { it.loginName == "venuehost resident" })
        assertFalse(saved.state.addAccountExpanded)
        assertFalse(saved.state.editAccountExpanded)
        assertFalse(saved.state.deleteExpanded)
        assertEquals("", saved.state.addUsernameDraft)
        assertEquals("", saved.state.addPasswordDraft)
        assertFalse(saved.state.newPasswordVisible)
        assertFalse(saved.state.saveNewAccountEnabled)
    }

    @Test
    fun accountPanelsCollapseEachOtherAndClearDeleteSelection() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
        val deleteOpen = AccountsController(runtime)
            .refreshSavedAccounts()
            .openDeleteAccounts()
            .setDeleteAccountSelected(profile.profileId, true)

        val addOpen = deleteOpen.toggleAddAccountPanel()
        assertTrue(addOpen.state.addAccountExpanded)
        assertFalse(addOpen.state.editAccountExpanded)
        assertFalse(addOpen.state.deleteExpanded)
        assertEquals(emptySet(), addOpen.state.selectedDeleteProfileIds)
        assertFalse(addOpen.state.confirmDeleteOpen)

        val editOpen = addOpen.toggleEditAccountPanel()
        assertTrue(editOpen.state.editAccountExpanded)
        assertFalse(editOpen.state.addAccountExpanded)
        assertFalse(editOpen.state.deleteExpanded)

        val deleteReopened = editOpen.openDeleteAccounts()
        assertTrue(deleteReopened.state.deleteExpanded)
        assertFalse(deleteReopened.state.editAccountExpanded)
        assertFalse(deleteReopened.state.addAccountExpanded)
        assertEquals(emptySet(), deleteReopened.state.selectedDeleteProfileIds)
    }

    @Test
    fun deleteAccountRequiresModalAndClearsDeletedActiveLabelOnConfirmOnly() {
        val runtime = FakeGemUiRuntime.ready()
        val profile = FakeGemUiRuntime.defaultProfile()
        val selected = AccountsController(
            runtime = runtime,
            appState = AppUiState(
                route = UiRoute.Accounts,
                activeAccountLabel = profile.loginName.value,
            ),
        )
            .refreshSavedAccounts()
            .openDeleteAccounts()
            .setDeleteAccountSelected(profile.profileId, true)
        val modalOpen = selected.openDeleteAccounts()
        val cancelled = modalOpen.cancelDeleteAccounts()
        val confirmed = modalOpen.confirmDeleteAccounts()

        assertTrue(modalOpen.state.confirmDeleteOpen)
        assertFalse(cancelled.state.confirmDeleteOpen)
        assertTrue(cancelled.state.savedLoginOptions.any { it.profileId == profile.profileId })
        assertFalse(confirmed.state.confirmDeleteOpen)
        assertFalse(confirmed.state.savedLoginOptions.any { it.profileId == profile.profileId })
        assertEquals(emptySet(), confirmed.state.selectedDeleteProfileIds)
        assertTrue(confirmed.state.addAccountExpanded)
        assertFalse(confirmed.state.editAccountExpanded)
        assertFalse(confirmed.state.deleteExpanded)
        assertEquals("", confirmed.appState.activeAccountLabel)
    }

    @Test
    fun deleteActionExpandsFirstAndDoesNotOpenModalWithoutSelection() {
        val runtime = FakeGemUiRuntime.ready()
        val controller = AccountsController(runtime)
            .refreshSavedAccounts()
            .openDeleteAccounts()

        assertTrue(controller.state.deleteExpanded)
        assertFalse(controller.state.confirmDeleteOpen)
        assertFalse(controller.state.deleteEnabled)
    }

    @Test
    fun expandedDeleteActionWithoutSelectionCollapsesPanel() {
        val collapsed = AccountsController(FakeGemUiRuntime.ready())
            .refreshSavedAccounts()
            .openDeleteAccounts()
            .openDeleteAccounts()

        assertFalse(collapsed.state.deleteExpanded)
        assertFalse(collapsed.state.confirmDeleteOpen)
    }

    @Test
    fun unavailableCredentialRuntimeDisablesAccountMutations() {
        val controller = AccountsController(FakeGemUiRuntime.unavailable())
            .refreshSavedAccounts()
            .updateNewUsernameDraft("venuehost")
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertFalse(controller.state.passwordEnabled)
        assertFalse(controller.state.saveNewAccountEnabled)
        assertEquals(GemTextKey.BlankStatus, controller.state.errorKey)
    }
}
