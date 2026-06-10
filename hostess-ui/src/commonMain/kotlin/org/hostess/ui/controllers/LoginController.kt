package org.hostess.ui.controllers

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.services.CredentialServiceAddResult
import org.hostess.core.services.CredentialServiceRevealPasswordResult
import org.hostess.core.services.SavedLoginAuthenticationResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.LoginUiState
import org.hostess.ui.state.SavedLoginOptionUiState
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.text.HostessTextKey

class LoginController(
    val runtime: HostessUiRuntime,
    val state: LoginUiState = LoginUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
    val appState: AppUiState = AppUiState(),
) {
    fun refreshSavedLogins(): LoginController {
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

    fun selectSavedLogin(profileId: AccountProfileId?): LoginController =
        if (profileId == null) {
            copy(
                state.copy(
                    selectedProfileId = null,
                    passwordDraft = "",
                    passwordVisible = false,
                    passwordEnabled = false,
                    loginEnabled = false,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
        } else {
            revealSavedPassword(profileId)
        }

    fun toggleSavedPasswordVisibility(): LoginController {
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun updateSavedPasswordDraft(passwordDraft: String): LoginController =
        copy(state.copy(passwordDraft = passwordDraft, errorKey = null, errorMessage = null))

    fun toggleAddLoginPanel(): LoginController =
        copy(state.copy(addLoginExpanded = !state.addLoginExpanded))

    fun updateNewUsernameDraft(username: String): LoginController =
        copy(state.withNewLoginDrafts(username = username))

    fun updateNewPasswordDraft(password: String): LoginController =
        copy(state.withNewLoginDrafts(password = password))

    fun toggleNewPasswordVisibility(): LoginController =
        copy(state.copy(newPasswordVisible = !state.newPasswordVisible))

    fun normalizeNewLoginNameOnPasswordFocus(): LoginController =
        when (val result = SecondLifeLoginName.fromUserInput(state.newUsernameDraft)) {
            is SecondLifeLoginNameResult.Valid -> copy(state.withNewLoginDrafts(username = result.loginName.value))
            is SecondLifeLoginNameResult.Invalid -> copy(
                state.copy(
                    saveAndLoginEnabled = false,
                    errorKey = HostessTextKey.BlankStatus,
                    errorMessage = result.reason.name,
                ),
            )
        }

    fun saveAndLogin(): LoginController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        if (!state.saveAndLoginEnabled) {
            return copy(state)
        }
        return when (val added = credentialService.addLogin(state.newUsernameDraft, state.newPasswordDraft)) {
            is CredentialServiceAddResult.Saved -> authenticate(
                profile = added.profile,
                passwordDraft = state.newPasswordDraft,
                clearNewLoginDrafts = true,
            )
            is CredentialServiceAddResult.InvalidLoginName -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = added.reason.name),
            )
            CredentialServiceAddResult.InvalidSecret -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = null),
            )
            is CredentialServiceAddResult.ProfileStoreFailure -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = added.message),
            )
            is CredentialServiceAddResult.VaultFailure -> copy(
                state.copy(errorKey = HostessTextKey.BlankStatus, errorMessage = added.message),
            )
        }
    }

    fun loginSelected(): LoginController {
        val profileId = state.selectedProfileId
            ?: return copy(state)
        val profile = profileFor(profileId)
            ?: return copy(
                state.copy(
                    errorKey = HostessTextKey.BlankStatus,
                    errorMessage = null,
                ),
            )
        return authenticate(profile, state.passwordDraft, clearNewLoginDrafts = false)
    }

    private fun revealSavedPassword(profileId: AccountProfileId): LoginController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val revealed = credentialService.revealPassword(profileId)) {
            is CredentialServiceRevealPasswordResult.Revealed -> copy(
                state.copy(
                    selectedProfileId = profileId,
                    passwordDraft = revealed.password,
                    passwordVisible = false,
                    passwordEnabled = true,
                    loginEnabled = true,
                    errorKey = null,
                    errorMessage = null,
                ),
            )
            is CredentialServiceRevealPasswordResult.MissingProfile -> copy(
                state.copy(
                    selectedProfileId = profileId,
                    passwordDraft = "",
                    passwordVisible = false,
                    passwordEnabled = false,
                    loginEnabled = false,
                    errorKey = HostessTextKey.BlankStatus,
                    errorMessage = null,
                ),
            )
            is CredentialServiceRevealPasswordResult.ProfileStoreFailure -> revealFailure(profileId, revealed.message)
            is CredentialServiceRevealPasswordResult.VaultFailure -> revealFailure(profileId, revealed.message)
        }
    }

    private fun authenticate(
        profile: SavedAccountProfile,
        passwordDraft: String,
        clearNewLoginDrafts: Boolean,
    ): LoginController {
        val authenticationService = runtime.savedLoginAuthenticationServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        val compliance = runtime.loginComplianceProvider.requestFor(profile)
        return when (
            val result = authenticationService.loginWithSavedProfile(profile.profileId, passwordDraft, compliance)
        ) {
            is SavedLoginAuthenticationResult.Success -> routeAfterReadiness(
                profile = result.profile,
                session = result.session,
                clearNewLoginDrafts = clearNewLoginDrafts,
            )
            SavedLoginAuthenticationResult.InvalidPasswordDraft -> authenticationFailure(null)
            is SavedLoginAuthenticationResult.LoginFailed -> authenticationFailure(result.failure.redactedMessage)
            is SavedLoginAuthenticationResult.MissingProfile -> authenticationFailure(null)
            is SavedLoginAuthenticationResult.RestoreFailed -> authenticationFailure(result.restoreMessage)
            is SavedLoginAuthenticationResult.RevealFailed -> authenticationFailure(result.message)
            is SavedLoginAuthenticationResult.UpdateFailed -> authenticationFailure(result.message)
        }
    }

    private fun routeAfterReadiness(
        profile: SavedAccountProfile,
        session: HostessSession,
        clearNewLoginDrafts: Boolean,
    ): LoginController =
        when (val readiness = runtime.avatarReadinessService.ensureReady(session)) {
            is AvatarReadinessResult.Success -> copy(
                state = state.afterSuccessfulLogin(clearNewLoginDrafts),
                appState = appState.copy(
                    route = UiRoute.Compose,
                    menuOpen = false,
                    activeAccountLabel = profile.loginName.value,
                    sessionStrip = SessionStripUiState(
                        visible = true,
                        locationLabel = profile.startLocation.orEmpty(),
                        statusKey = HostessTextKey.Online,
                        online = true,
                    ),
                    operationMessageKey = HostessTextKey.Ready,
                    session = session,
                ),
            )
            is AvatarReadinessResult.Failure -> authenticationFailure(readiness.failure.redactedMessage)
        }

    private fun LoginUiState.afterSuccessfulLogin(clearNewLoginDrafts: Boolean): LoginUiState {
        val base = copy(
            selectedProfileId = null,
            passwordDraft = "",
            passwordVisible = false,
            passwordEnabled = false,
            loginEnabled = false,
            errorKey = null,
            errorMessage = null,
        )
        return if (!clearNewLoginDrafts) {
            base
        } else {
            base.copy(
                newUsernameDraft = "",
                newPasswordDraft = "",
                newPasswordVisible = false,
                saveAndLoginEnabled = false,
            )
        }
    }

    private fun LoginUiState.withNewLoginDrafts(
        username: String = newUsernameDraft,
        password: String = newPasswordDraft,
    ): LoginUiState =
        copy(
            newUsernameDraft = username,
            newPasswordDraft = password,
            saveAndLoginEnabled = credentialRuntime.ready && username.isNotBlank() && password.isNotBlank(),
            errorKey = null,
            errorMessage = null,
        )

    private fun profileFor(profileId: AccountProfileId): SavedAccountProfile? =
        when (val listed = runtime.credentialServiceOrNull()?.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> listed.profiles.firstOrNull { it.profileId == profileId }
            else -> null
        }

    private fun revealFailure(
        profileId: AccountProfileId,
        message: String?,
    ): LoginController =
        copy(
            state.copy(
                selectedProfileId = profileId,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                loginEnabled = false,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun authenticationFailure(message: String?): LoginController =
        copy(
            state.copy(
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
                newPasswordVisible = false,
            ),
        )

    private fun credentialRuntimeUnavailable(message: String? = state.credentialRuntime.message): LoginController =
        copy(
            state.copy(
                selectedProfileId = null,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                loginEnabled = false,
                saveAndLoginEnabled = false,
                errorKey = HostessTextKey.BlankStatus,
                errorMessage = message,
            ),
        )

    private fun copy(
        state: LoginUiState = this.state,
        appState: AppUiState = this.appState,
    ): LoginController =
        LoginController(runtime, state, appState)
}
