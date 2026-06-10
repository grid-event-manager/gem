package org.hostess.ui.controllers

import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.SettingsUiState

class SettingsController(
    val runtime: HostessUiRuntime,
    val state: SettingsUiState = SettingsUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
) {
    fun selectSavedAccount(profileId: AccountProfileId?): SettingsController =
        copy(
            state.copy(
                selectedProfileId = profileId,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = profileId != null,
                errorKey = null,
            ),
        )

    fun toggleSavedPasswordVisibility(): SettingsController {
        // B-08 owns CredentialService.revealPassword wiring for settings.
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun updateSavedPasswordDraft(passwordDraft: String): SettingsController {
        // B-08 owns CredentialService.updatePassword autosave; B-06 stores only the draft projection.
        return copy(state.copy(passwordDraft = passwordDraft, errorKey = null))
    }

    fun toggleAddAccountPanel(): SettingsController =
        copy(state.copy(addAccountExpanded = !state.addAccountExpanded))

    fun saveNewAccount(): SettingsController {
        // B-08 owns CredentialService.addLogin for the settings add-account flow.
        return copy(state)
    }

    fun openDeleteAccounts(): SettingsController =
        copy(
            state.copy(
                deleteExpanded = true,
                confirmDeleteOpen = state.selectedDeleteProfileIds.isNotEmpty(),
            ),
        )

    fun setDeleteAccountSelected(
        profileId: AccountProfileId,
        selected: Boolean,
    ): SettingsController {
        val selectedIds = if (selected) {
            state.selectedDeleteProfileIds + profileId
        } else {
            state.selectedDeleteProfileIds - profileId
        }
        return copy(state.copy(selectedDeleteProfileIds = selectedIds))
    }

    fun confirmDeleteAccounts(): SettingsController {
        // B-08 owns CredentialService.deleteProfiles; B-06 preserves modal state shape only.
        return copy(state.copy(confirmDeleteOpen = false))
    }

    fun cancelDeleteAccounts(): SettingsController =
        copy(state.copy(confirmDeleteOpen = false))

    private fun copy(state: SettingsUiState): SettingsController =
        SettingsController(runtime, state)
}
