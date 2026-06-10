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
    val addAccountExpanded: Boolean = false,
    val newUsernameDraft: String = "",
    val newPasswordDraft: String = "",
    val newPasswordVisible: Boolean = false,
    val deleteExpanded: Boolean = false,
    val selectedDeleteProfileIds: Set<AccountProfileId> = emptySet(),
    val confirmDeleteOpen: Boolean = false,
    val errorKey: HostessTextKey? = null,
) {
    companion object {
        fun fromCredentialRuntime(runtimeState: HostessCredentialRuntimeState): SettingsUiState =
            SettingsUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}
