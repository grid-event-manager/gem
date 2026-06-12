package org.gem.ui.state

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.ui.text.GemTextKey

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
        fun fromCredentialRuntime(runtimeState: GemCredentialRuntimeState): LoginUiState =
            LoginUiState(credentialRuntime = CredentialRuntimeUiState.from(runtimeState))
    }
}

data class LoginOperationUiState(
    val inFlight: Boolean,
    val messageKey: GemTextKey?,
    val errorKey: GemTextKey?,
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
