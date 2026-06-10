package org.hostess.ui.state

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.ui.text.HostessTextKey

data class LoginUiState(
    val credentialRuntime: CredentialRuntimeUiState,
    val savedLoginOptions: List<SavedLoginOptionUiState> = emptyList(),
    val selectedProfileId: AccountProfileId? = null,
    val passwordDraft: String = "",
    val passwordVisible: Boolean = false,
    val passwordEnabled: Boolean = false,
    val loginEnabled: Boolean = false,
    val addLoginExpanded: Boolean = false,
    val newUsernameDraft: String = "",
    val newPasswordDraft: String = "",
    val newPasswordVisible: Boolean = false,
    val saveAndLoginEnabled: Boolean = false,
    val errorKey: HostessTextKey? = null,
) {
    companion object {
        fun fromCredentialRuntime(runtimeState: HostessCredentialRuntimeState): LoginUiState =
            LoginUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}

data class SavedLoginOptionUiState(
    val profileId: AccountProfileId,
    val loginName: String,
    val label: String,
) {
    companion object {
        fun from(profile: SavedAccountProfile): SavedLoginOptionUiState =
            SavedLoginOptionUiState(
                profileId = profile.profileId,
                loginName = profile.loginName.value,
                label = profile.label,
            )
    }
}
