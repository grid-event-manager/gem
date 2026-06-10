package org.hostess.ui.controllers

import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsControllerTest {
    @Test
    fun selectingSavedAccountRevealsMaskedPasswordAndAutosavesEdits() {
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
        val revealedAgain = updated.selectSavedAccount(profile.profileId)

        assertEquals("changed-password", revealedAgain.state.passwordDraft)
        assertNull(revealedAgain.state.errorKey)
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
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertEquals(UiRoute.Settings, saved.appState.route)
        assertTrue(saved.state.savedLoginOptions.any { it.loginName == "venuehost resident" })
        assertFalse(saved.state.addAccountExpanded)
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
        assertEquals("", confirmed.appState.activeAccountLabel)
    }

    @Test
    fun deleteButtonDoesNotOpenModalWithoutSelection() {
        val runtime = FakeHostessUiRuntime.ready()
        val controller = SettingsController(runtime)
            .refreshSavedAccounts()
            .openDeleteAccounts()

        assertTrue(controller.state.deleteExpanded)
        assertFalse(controller.state.confirmDeleteOpen)
        assertFalse(controller.state.deleteEnabled)
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
