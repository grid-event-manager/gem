package org.hostess.ui.state

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.ui.text.HostessTextKey

data class LoginUiState(
    val credentialRuntime: CredentialRuntimeUiState,
    val savedLoginOptions: List<SavedLoginOptionUiState> = emptyList(),
    val usernameDraft: String = "",
    val selectedProfileId: AccountProfileId? = null,
    val entryMode: LoginEntryMode = LoginEntryMode.New,
    val passwordDraft: String = "",
    val passwordVisible: Boolean = false,
    val passwordEnabled: Boolean = false,
    val loginEnabled: Boolean = false,
    val operation: LoginOperationUiState = LoginOperationUiState.Idle,
) {
    companion object {
        fun fromCredentialRuntime(runtimeState: HostessCredentialRuntimeState): LoginUiState =
            LoginUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}

data class LoginOperationUiState(
    val inFlight: Boolean,
    val messageKey: HostessTextKey?,
    val errorKey: HostessTextKey?,
    val errorMessage: String?,
) {
    companion object {
        val Idle: LoginOperationUiState = LoginOperationUiState(
            inFlight = false,
            messageKey = null,
            errorKey = null,
            errorMessage = null,
        )
    }
}

sealed interface LoginEntryMode {
    data class Saved(val profileId: AccountProfileId) : LoginEntryMode
    data object New : LoginEntryMode
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
