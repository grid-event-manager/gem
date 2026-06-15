package org.gem.ui.controllers

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.services.CredentialServiceRevealPasswordResult
import org.gem.core.services.CredentialServiceUpdatePasswordResult
import org.gem.core.services.SavedAccountAddResult
import org.gem.core.services.SavedAccountDeleteResult
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.AppUiState
import org.gem.ui.state.SavedLoginOptionUiState
import org.gem.ui.state.AccountsUiState
import org.gem.ui.text.GemTextKey

class AccountsController(
    val runtime: GemUiRuntime,
    val state: AccountsUiState = AccountsUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
    val appState: AppUiState = AppUiState(),
) {
    fun refreshSavedAccounts(): AccountsController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val listed = credentialService.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> {
                val refreshed = copy(
                    state.copy(
                        savedLoginOptions = listed.profiles.map(SavedLoginOptionUiState::from),
                        selectedProfileId = state.selectedProfileId?.takeIf { selected ->
                            listed.profiles.any { it.profileId == selected }
                        },
                        addAccountExpanded = if (listed.profiles.isEmpty()) true else state.addAccountExpanded,
                        editAccountExpanded = if (listed.profiles.isEmpty()) false else state.editAccountExpanded,
                        deleteExpanded = if (listed.profiles.isEmpty()) false else state.deleteExpanded,
                        selectedDeleteProfileIds = if (listed.profiles.isEmpty()) {
                            emptySet()
                        } else {
                            state.selectedDeleteProfileIds
                        },
                        errorKey = null,
                        errorMessage = null,
                    ),
                )
                if (listed.profiles.isEmpty()) {
                    refreshed
                } else {
                    refreshed.selectPreferredSavedAccount(listed.profiles)
                }
            }
            is AccountProfileStoreListResult.CorruptVault -> credentialRuntimeUnavailable(listed.message)
            is AccountProfileStoreListResult.StorageFailed -> credentialRuntimeUnavailable(listed.message)
        }
    }

    fun selectSavedAccount(profileId: AccountProfileId?): AccountsController =
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

    fun toggleSavedPasswordVisibility(): AccountsController {
        if (!state.passwordEnabled) {
            return copy(state)
        }
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun updateSavedPasswordDraft(passwordDraft: String): AccountsController {
        if (state.selectedProfileId == null) {
            return copy(state.copy(passwordDraft = passwordDraft))
        }
        return copy(
            state.copy(
                passwordDraft = passwordDraft,
                errorKey = null,
                errorMessage = null,
            ),
        )
    }

    fun saveEditedPassword(): AccountsController {
        val profileId = state.selectedProfileId
            ?: return copy(state)
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val updated = credentialService.updatePassword(profileId, state.passwordDraft)) {
            CredentialServiceUpdatePasswordResult.Updated -> copy(
                state.copy(
                    editAccountExpanded = false,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            CredentialServiceUpdatePasswordResult.InvalidSecret -> updateFailure(state.passwordDraft)
            is CredentialServiceUpdatePasswordResult.MissingProfile -> updateFailure(state.passwordDraft)
            is CredentialServiceUpdatePasswordResult.ProfileStoreFailure -> updateFailure(state.passwordDraft, updated.message)
            is CredentialServiceUpdatePasswordResult.VaultFailure -> updateFailure(state.passwordDraft, updated.message)
        }
    }

    fun toggleEditAccountPanel(): AccountsController =
        copy(state.copy(editAccountExpanded = !state.editAccountExpanded))

    fun toggleAddAccountPanel(): AccountsController =
        copy(state.copy(addAccountExpanded = !state.addAccountExpanded))

    fun updateNewUsernameDraft(username: String): AccountsController =
        copy(state.withNewAccountDrafts(username = username))

    fun updateNewPasswordDraft(password: String): AccountsController =
        copy(state.withNewAccountDrafts(password = password))

    fun toggleNewPasswordVisibility(): AccountsController =
        copy(state.copy(newPasswordVisible = !state.newPasswordVisible))

    fun normalizeNewAccountNameOnPasswordFocus(): AccountsController =
        when (val result = SecondLifeLoginName.fromUserInput(state.addUsernameDraft)) {
            is SecondLifeLoginNameResult.Valid -> copy(state.withNewAccountDrafts(username = result.loginName.value))
            is SecondLifeLoginNameResult.Invalid -> copy(
                state.copy(
                    saveNewAccountEnabled = false,
                    errorKey = GemTextKey.BlankStatus,
                    errorMessage = result.reason.name,
                ),
            )
        }

    fun saveNewAccount(): AccountsController {
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
                        errorKey = GemTextKey.BlankStatus,
                        errorMessage = result.reason.name,
                    ),
                )
            }
        }
        return when (val added = accountManagementService.addAccount(normalizedName, state.addPasswordDraft)) {
            is SavedAccountAddResult.Saved -> copy(
                state.copy(
                    savedLoginOptions = refreshedOptions() ?: (state.savedLoginOptions + SavedLoginOptionUiState.from(added.profile)),
                    selectedProfileId = added.profile.profileId,
                    passwordDraft = state.addPasswordDraft,
                    passwordVisible = false,
                    passwordEnabled = true,
                    editAccountExpanded = false,
                    addAccountExpanded = false,
                    addUsernameDraft = "",
                    addPasswordDraft = "",
                    newPasswordVisible = false,
                    saveNewAccountEnabled = false,
                    deleteExpanded = false,
                    selectedDeleteProfileIds = emptySet(),
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            is SavedAccountAddResult.InvalidLoginName -> copy(
                state.copy(errorKey = GemTextKey.BlankStatus, errorMessage = added.reason.name),
            )
            SavedAccountAddResult.InvalidSecret -> copy(
                state.copy(errorKey = GemTextKey.BlankStatus, errorMessage = null),
            )
            is SavedAccountAddResult.CredentialStoreFailed -> copy(
                state.copy(errorKey = GemTextKey.BlankStatus, errorMessage = added.message),
            )
        }
    }

    fun openDeleteAccounts(): AccountsController =
        when {
            !state.deleteExpanded -> copy(state.copy(deleteExpanded = true, confirmDeleteOpen = false))
            state.deleteEnabled -> copy(state.copy(confirmDeleteOpen = true))
            else -> copy(state.copy(deleteExpanded = false, confirmDeleteOpen = false))
        }

    fun setDeleteAccountSelected(
        profileId: AccountProfileId,
        selected: Boolean,
    ): AccountsController {
        val selectedIds = if (selected) {
            state.selectedDeleteProfileIds + profileId
        } else {
            state.selectedDeleteProfileIds - profileId
        }
        return copy(state.copy(selectedDeleteProfileIds = selectedIds, errorKey = null, errorMessage = null))
    }

    fun confirmDeleteAccounts(): AccountsController {
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

    fun cancelDeleteAccounts(): AccountsController =
        copy(state.copy(confirmDeleteOpen = false))

    private fun revealSavedPassword(profileId: AccountProfileId): AccountsController {
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

    private fun AccountsUiState.withNewAccountDrafts(
        username: String = addUsernameDraft,
        password: String = addPasswordDraft,
    ): AccountsUiState =
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
    ): AccountsController {
        val activeDeleted = appState.activeAccountLabel in deletedLoginNames
        val remainingOptions = refreshedOptions()
            ?: state.savedLoginOptions.filterNot { it.profileId in deletedProfileIds }
        val selectedProfileId = state.selectedProfileId?.takeUnless { it in deletedProfileIds }
        return copy(
            state.copy(
                savedLoginOptions = remainingOptions,
                selectedProfileId = selectedProfileId,
                passwordDraft = if (state.selectedProfileId in deletedProfileIds) "" else state.passwordDraft,
                passwordVisible = if (state.selectedProfileId in deletedProfileIds) false else state.passwordVisible,
                passwordEnabled = if (state.selectedProfileId in deletedProfileIds) false else state.passwordEnabled,
                editAccountExpanded = false,
                addAccountExpanded = if (remainingOptions.isEmpty()) true else state.addAccountExpanded,
                deleteExpanded = false,
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

    private fun selectPreferredSavedAccount(profiles: List<SavedAccountProfile>): AccountsController {
        if (state.selectedProfileId != null && profiles.any { it.profileId == state.selectedProfileId }) {
            return revealSavedPassword(state.selectedProfileId)
        }
        val preferredProfileId = runtime.lastLoginProfilePreferenceService.loadPreference().profileId
        val preferredProfile = profiles.firstOrNull { it.profileId == preferredProfileId }
            ?: profiles.firstOrNull()
            ?: return this
        return revealSavedPassword(preferredProfile.profileId)
    }

    private fun updateFailure(
        passwordDraft: String,
        message: String? = null,
    ): AccountsController =
        copy(
            state.copy(
                passwordDraft = passwordDraft,
                errorKey = GemTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun revealFailure(
        profileId: AccountProfileId,
        message: String? = null,
    ): AccountsController =
        copy(
            state.copy(
                selectedProfileId = profileId,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                errorKey = GemTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun deleteFailure(message: String? = null): AccountsController =
        copy(
            state.copy(
                confirmDeleteOpen = true,
                errorKey = GemTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun credentialRuntimeUnavailable(message: String? = state.credentialRuntime.message): AccountsController =
        copy(
            state.copy(
                selectedProfileId = null,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                editAccountExpanded = false,
                deleteExpanded = false,
                selectedDeleteProfileIds = emptySet(),
                saveNewAccountEnabled = false,
                confirmDeleteOpen = false,
                errorKey = GemTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun copy(
        state: AccountsUiState = this.state,
        appState: AppUiState = this.appState,
    ): AccountsController =
        AccountsController(runtime, state, appState)
}
