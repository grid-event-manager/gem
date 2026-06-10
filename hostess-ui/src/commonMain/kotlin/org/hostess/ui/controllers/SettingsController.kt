package org.hostess.ui.controllers

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.services.CredentialServiceRevealPasswordResult
import org.hostess.core.services.CredentialServiceUpdatePasswordResult
import org.hostess.core.services.SavedAccountAddResult
import org.hostess.core.services.SavedAccountDeleteResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.SavedLoginOptionUiState
import org.hostess.ui.state.SettingsUiState
import org.hostess.ui.text.HostessTextKey

class SettingsController(
    val runtime: HostessUiRuntime,
    val state: SettingsUiState = SettingsUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
    val appState: AppUiState = AppUiState(),
) {
    fun refreshSavedAccounts(): SettingsController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val listed = credentialService.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> copy(
                state.copy(
                    savedLoginOptions = listed.profiles.map(SavedLoginOptionUiState::from),
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            is AccountProfileStoreListResult.CorruptVault -> credentialRuntimeUnavailable(listed.message)
            is AccountProfileStoreListResult.StorageFailed -> credentialRuntimeUnavailable(listed.message)
        }
    }

    fun selectSavedAccount(profileId: AccountProfileId?): SettingsController =
        if (profileId == null) {
            copy(
                state.copy(
                    selectedProfileId = null,
                    passwordDraft = "",
                    passwordVisible = false,
                    passwordEnabled = false,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
        } else {
            revealSavedPassword(profileId)
        }

    fun toggleSavedPasswordVisibility(): SettingsController {
        if (!state.passwordEnabled) {
            return copy(state)
        }
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun updateSavedPasswordDraft(passwordDraft: String): SettingsController {
        val profileId = state.selectedProfileId
            ?: return copy(state.copy(passwordDraft = passwordDraft))
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val updated = credentialService.updatePassword(profileId, passwordDraft)) {
            CredentialServiceUpdatePasswordResult.Updated -> copy(
                state.copy(
                    passwordDraft = passwordDraft,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            CredentialServiceUpdatePasswordResult.InvalidSecret -> updateFailure(passwordDraft)
            is CredentialServiceUpdatePasswordResult.MissingProfile -> updateFailure(passwordDraft)
            is CredentialServiceUpdatePasswordResult.ProfileStoreFailure -> updateFailure(passwordDraft, updated.message)
            is CredentialServiceUpdatePasswordResult.VaultFailure -> updateFailure(passwordDraft, updated.message)
        }
    }

    fun toggleAddAccountPanel(): SettingsController =
        copy(state.copy(addAccountExpanded = !state.addAccountExpanded))

    fun updateNewUsernameDraft(username: String): SettingsController =
        copy(state.withNewAccountDrafts(username = username))

    fun updateNewPasswordDraft(password: String): SettingsController =
        copy(state.withNewAccountDrafts(password = password))

    fun toggleNewPasswordVisibility(): SettingsController =
        copy(state.copy(newPasswordVisible = !state.newPasswordVisible))

    fun normalizeNewAccountNameOnPasswordFocus(): SettingsController =
        when (val result = SecondLifeLoginName.fromUserInput(state.addUsernameDraft)) {
            is SecondLifeLoginNameResult.Valid -> copy(state.withNewAccountDrafts(username = result.loginName.value))
            is SecondLifeLoginNameResult.Invalid -> copy(
                state.copy(
                    saveNewAccountEnabled = false,
                    errorKey = HostessTextKey.BlankStatus,
                    errorMessage = result.reason.name,
                ),
            )
        }

    fun saveNewAccount(): SettingsController {
        val accountManagementService = runtime.savedAccountManagementServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        if (!state.saveNewAccountEnabled) {
            return copy(state)
        }
        val normalizedName = when (val result = SecondLifeLoginName.fromUserInput(state.addUsernameDraft)) {
            is SecondLifeLoginNameResult.Valid -> result.loginName.value
            is SecondLifeLoginNameResult.Invalid -> {
                return copy(
                    state.copy(
                        saveNewAccountEnabled = false,
                        errorKey = HostessTextKey.BlankStatus,
                        errorMessage = result.reason.name,
                    ),
                )
            }
        }
        return when (val added = accountManagementService.addAccount(normalizedName, state.addPasswordDraft)) {
            is SavedAccountAddResult.Saved -> copy(
                state.copy(
                    savedLoginOptions = refreshedOptions() ?: (state.savedLoginOptions + SavedLoginOptionUiState.from(added.profile)),
                    addAccountExpanded = false,
                    addUsernameDraft = "",
                    addPasswordDraft = "",
                    newPasswordVisible = false,
                    saveNewAccountEnabled = false,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            is SavedAccountAddResult.InvalidLoginName -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = added.reason.name),
            )
            SavedAccountAddResult.InvalidSecret -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = null),
            )
            is SavedAccountAddResult.CredentialStoreFailed -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = added.message),
            )
        }
    }

    fun toggleDeleteAccountPanel(): SettingsController =
        copy(state.copy(deleteExpanded = !state.deleteExpanded))

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
        return copy(state.copy(selectedDeleteProfileIds = selectedIds, errorKey = null, errorMessage = null))
    }

    fun confirmDeleteAccounts(): SettingsController {
        val selectedIds = state.selectedDeleteProfileIds
        if (selectedIds.isEmpty()) {
            return copy(state.copy(confirmDeleteOpen = false))
        }
        val accountManagementService = runtime.savedAccountManagementServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        val deletedLoginNames = state.savedLoginOptions
            .filter { it.profileId in selectedIds }
            .map { it.loginName }
            .toSet()
        return when (val deleted = accountManagementService.deleteAccounts(selectedIds)) {
            is SavedAccountDeleteResult.Deleted -> afterDeleteSuccess(deleted.profileIds, deletedLoginNames)
            is SavedAccountDeleteResult.MissingProfiles -> deleteFailure()
            is SavedAccountDeleteResult.CredentialStoreFailed -> deleteFailure(deleted.message)
        }
    }

    fun cancelDeleteAccounts(): SettingsController =
        copy(state.copy(confirmDeleteOpen = false))

    private fun revealSavedPassword(profileId: AccountProfileId): SettingsController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val revealed = credentialService.revealPassword(profileId)) {
            is CredentialServiceRevealPasswordResult.Revealed -> copy(
                state.copy(
                    selectedProfileId = profileId,
                    passwordDraft = revealed.password,
                    passwordVisible = false,
                    passwordEnabled = true,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            is CredentialServiceRevealPasswordResult.MissingProfile -> revealFailure(profileId)
            is CredentialServiceRevealPasswordResult.ProfileStoreFailure -> revealFailure(profileId, revealed.message)
            is CredentialServiceRevealPasswordResult.VaultFailure -> revealFailure(profileId, revealed.message)
        }
    }

    private fun SettingsUiState.withNewAccountDrafts(
        username: String = addUsernameDraft,
        password: String = addPasswordDraft,
    ): SettingsUiState =
        copy(
            addUsernameDraft = username,
            addPasswordDraft = password,
            saveNewAccountEnabled = credentialRuntime.ready && username.isNotBlank() && password.isNotBlank(),
            errorKey = null,
            errorMessage = null,
        )

    private fun afterDeleteSuccess(
        deletedProfileIds: Set<AccountProfileId>,
        deletedLoginNames: Set<String>,
    ): SettingsController {
        val activeDeleted = appState.activeAccountLabel in deletedLoginNames
        return copy(
            state.copy(
                savedLoginOptions = refreshedOptions() ?: state.savedLoginOptions.filterNot { it.profileId in deletedProfileIds },
                selectedProfileId = state.selectedProfileId?.takeUnless { it in deletedProfileIds },
                passwordDraft = if (state.selectedProfileId in deletedProfileIds) "" else state.passwordDraft,
                passwordVisible = if (state.selectedProfileId in deletedProfileIds) false else state.passwordVisible,
                passwordEnabled = if (state.selectedProfileId in deletedProfileIds) false else state.passwordEnabled,
                selectedDeleteProfileIds = emptySet(),
                confirmDeleteOpen = false,
                errorKey = null,
                errorMessage = null,
            ),
            appState = if (activeDeleted) appState.copy(activeAccountLabel = "") else appState,
        )
    }

    private fun refreshedOptions(): List<SavedLoginOptionUiState>? =
        when (val listed = runtime.credentialServiceOrNull()?.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> listed.profiles.map(SavedLoginOptionUiState::from)
            else -> null
        }

    private fun updateFailure(
        passwordDraft: String,
        message: String? = null,
    ): SettingsController =
        copy(
            state.copy(
                passwordDraft = passwordDraft,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun revealFailure(
        profileId: AccountProfileId,
        message: String? = null,
    ): SettingsController =
        copy(
            state.copy(
                selectedProfileId = profileId,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun deleteFailure(message: String? = null): SettingsController =
        copy(
            state.copy(
                confirmDeleteOpen = true,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun credentialRuntimeUnavailable(message: String? = state.credentialRuntime.message): SettingsController =
        copy(
            state.copy(
                selectedProfileId = null,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                saveNewAccountEnabled = false,
                confirmDeleteOpen = false,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun copy(
        state: SettingsUiState = this.state,
        appState: AppUiState = this.appState,
    ): SettingsController =
        SettingsController(runtime, state, appState)
}
