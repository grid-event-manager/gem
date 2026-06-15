package org.gem.ui.state

import org.gem.core.domain.AccountProfileId
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.ui.text.GemTextKey

data class AccountsUiState(
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
    val errorKey: GemTextKey? = null,
    val errorMessage: String? = null,
) {
    val deleteEnabled: Boolean
        get() = selectedDeleteProfileIds.isNotEmpty()

    val saveEditedPasswordEnabled: Boolean
        get() = passwordEnabled && passwordDraft.isNotBlank()

    companion object {
        fun fromCredentialRuntime(runtimeState: GemCredentialRuntimeState): AccountsUiState =
            AccountsUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}
