package org.hostess.ui.controllers

import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeLastLoginProfilePreferenceStore
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsControllerTest {
    @Test
    fun selectingSavedAccountRevealsMaskedPasswordAndSavesEditsExplicitly() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val selected = SettingsController(runtime)
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
        val profile = FakeHostessUiRuntime.defaultProfile()
        val runtime = FakeHostessUiRuntime.ready(
            lastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(profile.profileId),
        )
        val refreshed = SettingsController(runtime).refreshSavedAccounts()

        assertEquals(profile.profileId, refreshed.state.selectedProfileId)
        assertEquals("test-password", refreshed.state.passwordDraft)
        assertTrue(refreshed.state.passwordEnabled)
    }

    @Test
    fun settingsWithNoSavedAccountsExpandsAddAccountAndHidesDeleteState() {
        val refreshed = SettingsController(FakeHostessUiRuntime.ready(profiles = emptyList()))
            .refreshSavedAccounts()

        assertTrue(refreshed.state.savedLoginOptions.isEmpty())
        assertTrue(refreshed.state.addAccountExpanded)
        assertFalse(refreshed.state.editAccountExpanded)
        assertFalse(refreshed.state.deleteExpanded)
        assertFalse(refreshed.state.deleteEnabled)
    }

    @Test
    fun placeholderSelectionClearsAndDisablesSavedPassword() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val cleared = SettingsController(runtime)
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
        val runtime = FakeHostessUiRuntime.ready()
        val saved = SettingsController(
            runtime = runtime,
            appState = AppUiState(route = UiRoute.Settings),
        )
            .refreshSavedAccounts()
            .toggleAddAccountPanel()
            .updateNewUsernameDraft("venuehost")
            .normalizeNewAccountNameOnPasswordFocus()
            .openDeleteAccounts()
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertEquals(UiRoute.Settings, saved.appState.route)
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
    fun deleteAccountRequiresModalAndClearsDeletedActiveLabelOnConfirmOnly() {
        val runtime = FakeHostessUiRuntime.ready()
        val profile = FakeHostessUiRuntime.defaultProfile()
        val selected = SettingsController(
            runtime = runtime,
            appState = AppUiState(
                route = UiRoute.Settings,
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
        val runtime = FakeHostessUiRuntime.ready()
        val controller = SettingsController(runtime)
            .refreshSavedAccounts()
            .openDeleteAccounts()

        assertTrue(controller.state.deleteExpanded)
        assertFalse(controller.state.confirmDeleteOpen)
        assertFalse(controller.state.deleteEnabled)
    }

    @Test
    fun expandedDeleteActionWithoutSelectionCollapsesPanel() {
        val collapsed = SettingsController(FakeHostessUiRuntime.ready())
            .refreshSavedAccounts()
            .openDeleteAccounts()
            .openDeleteAccounts()

        assertFalse(collapsed.state.deleteExpanded)
        assertFalse(collapsed.state.confirmDeleteOpen)
    }

    @Test
    fun unavailableCredentialRuntimeDisablesSettingsMutations() {
        val controller = SettingsController(FakeHostessUiRuntime.unavailable())
            .refreshSavedAccounts()
            .updateNewUsernameDraft("venuehost")
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertFalse(controller.state.passwordEnabled)
        assertFalse(controller.state.saveNewAccountEnabled)
        assertEquals(HostessTextKey.BlankStatus, controller.state.errorKey)
    }
}
