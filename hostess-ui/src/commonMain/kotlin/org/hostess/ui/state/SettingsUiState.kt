package org.hostess.ui.state

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.ui.text.HostessTextKey

data class SettingsUiState(
    val credentialRuntime: CredentialRuntimeUiState,
    val savedLoginOptions: List<SavedLoginOptionUiState> = emptyList(),
    val selectedProfileId: AccountProfileId? = null,
    val passwordDraft: String = "",
    val passwordVisible: Boolean = false,
    val passwordEnabled: Boolean = false,
    val editAccountExpanded: Boolean = false,
    val addAccountExpanded: Boolean = false,
    val addUsernameDraft: String = "",
    val addPasswordDraft: String = "",
    val newPasswordVisible: Boolean = false,
    val saveNewAccountEnabled: Boolean = false,
    val deleteExpanded: Boolean = false,
    val selectedDeleteProfileIds: Set<AccountProfileId> = emptySet(),
    val confirmDeleteOpen: Boolean = false,
    val errorKey: HostessTextKey? = null,
    val errorMessage: String? = null,
) {
    val deleteEnabled: Boolean
        get() = selectedDeleteProfileIds.isNotEmpty()

    val saveEditedPasswordEnabled: Boolean
        get() = passwordEnabled && passwordDraft.isNotBlank()

    companion object {
        fun fromCredentialRuntime(runtimeState: HostessCredentialRuntimeState): SettingsUiState =
            SettingsUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}
