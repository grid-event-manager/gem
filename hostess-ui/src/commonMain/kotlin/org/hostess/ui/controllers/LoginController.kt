package org.hostess.ui.controllers

import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.LoginUiState

class LoginController(
    val runtime: HostessUiRuntime,
    val state: LoginUiState = LoginUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
) {
    fun selectSavedLogin(profileId: AccountProfileId?): LoginController =
        copy(
            state.copy(
                selectedProfileId = profileId,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = profileId != null,
                loginEnabled = profileId != null,
                errorKey = null,
            ),
        )

    fun toggleSavedPasswordVisibility(): LoginController {
        // B-07 owns CredentialService.revealPassword wiring; B-06 toggles only the field projection.
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun updateSavedPasswordDraft(passwordDraft: String): LoginController =
        copy(state.copy(passwordDraft = passwordDraft, errorKey = null))

    fun toggleAddLoginPanel(): LoginController =
        copy(state.copy(addLoginExpanded = !state.addLoginExpanded))

    fun normalizeNewLoginNameOnPasswordFocus(): LoginController {
        // B-07 owns SecondLifeLoginName.fromUserInput delegation for the add-login flow.
        return copy(state)
    }

    fun saveAndLogin(): LoginController {
        // B-07 owns CredentialService.addLogin plus SessionService.login sequencing.
        return copy(state)
    }

    fun loginSelected(): LoginController {
        // B-07 owns SavedLoginAuthenticationService and avatar-readiness routing.
        return copy(state)
    }

    private fun copy(state: LoginUiState): LoginController =
        LoginController(runtime, state)
}
