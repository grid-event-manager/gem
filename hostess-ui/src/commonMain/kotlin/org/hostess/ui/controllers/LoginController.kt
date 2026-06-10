package org.hostess.ui.controllers

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.services.CredentialServiceRevealPasswordResult
import org.hostess.core.services.LoginProfileAuthenticationResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.AppUiState
import org.hostess.ui.state.LoginEntryMode
import org.hostess.ui.state.LoginOperationUiState
import org.hostess.ui.state.LoginUiState
import org.hostess.ui.state.SavedLoginOptionUiState
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.state.UiRoute
import org.hostess.ui.text.HostessTextKey

class LoginController(
    val runtime: HostessUiRuntime,
    val state: LoginUiState = LoginUiState.fromCredentialRuntime(runtime.credentialRuntimeState),
    val appState: AppUiState = AppUiState(),
    private val authenticatedProfile: SavedAccountProfile? = null,
) {
    fun refreshSavedLogins(): LoginController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        return when (val listed = credentialService.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> copy(
                state.copy(
                    savedLoginOptions = listed.profiles.map(SavedLoginOptionUiState::from),
                    operation = state.operation.clearError(),
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
                    usernameDraft = "",
                    selectedProfileId = null,
                    entryMode = LoginEntryMode.New,
                    passwordDraft = "",
                    passwordVisible = false,
                    passwordEnabled = false,
                    loginEnabled = false,
                    operation = state.operation.clearError(),
                ),
            )
        } else {
            revealSavedPassword(profileId)
        }

    fun updateUsernameDraft(username: String): LoginController =
        copy(
            state.copy(
                usernameDraft = username,
                selectedProfileId = null,
                entryMode = LoginEntryMode.New,
                passwordEnabled = state.credentialRuntime.ready && username.isNotBlank(),
                loginEnabled = state.credentialRuntime.ready && username.isNotBlank() && state.passwordDraft.isNotBlank(),
                operation = state.operation.clearError(),
            ),
        )

    fun normalizeLoginNameOnPasswordFocus(): LoginController =
        if (state.entryMode is LoginEntryMode.Saved || state.usernameDraft.isBlank()) {
            copy(state.copy(operation = state.operation.clearError()))
        } else {
            when (val result = SecondLifeLoginName.fromUserInput(state.usernameDraft)) {
                is SecondLifeLoginNameResult.Valid -> updateUsernameDraft(result.loginName.value)
                is SecondLifeLoginNameResult.Invalid -> copy(
                    state.copy(
                        loginEnabled = false,
                        operation = LoginOperationUiState.error(
                            errorKey = HostessTextKey.LoginFailed,
                            errorMessage = result.reason.name,
                        ),
                    ),
                )
            }
        }

    fun updatePasswordDraft(passwordDraft: String): LoginController =
        copy(
            state.copy(
                passwordDraft = passwordDraft,
                loginEnabled = state.credentialRuntime.ready &&
                    state.usernameDraft.isNotBlank() &&
                    passwordDraft.isNotBlank() &&
                    state.passwordEnabled,
                operation = state.operation.clearError(),
            ),
        )

    fun togglePasswordVisibility(): LoginController {
        if (state.operation.inFlight || !state.passwordEnabled) {
            return copy(state)
        }
        return copy(state.copy(passwordVisible = !state.passwordVisible))
    }

    fun beginLogin(): LoginController {
        if (!state.loginEnabled || state.operation.inFlight) {
            return copy(state)
        }
        val initialMessage = if (state.entryMode is LoginEntryMode.New) {
            HostessTextKey.SavingLogin
        } else {
            HostessTextKey.LoggingIn
        }
        return copy(state.copy(operation = LoginOperationUiState.inFlight(initialMessage)))
    }

    fun showOperation(messageKey: HostessTextKey): LoginController =
        copy(state.copy(operation = LoginOperationUiState.inFlight(messageKey)))

    fun completeAuthentication(): LoginController {
        if (state.passwordDraft.isBlank() || state.usernameDraft.isBlank()) {
            return authenticationFailure(null)
        }
        val authenticationService = runtime.loginProfileAuthenticationServiceOrNull()
            ?: return credentialRuntimeUnavailable()

        val authentication = when (val mode = state.entryMode) {
            is LoginEntryMode.Saved -> {
                val profile = profileFor(mode.profileId)
                    ?: return credentialStoreFailure(null)
                authenticationService.loginSavedProfile(
                    profileId = mode.profileId,
                    passwordDraft = state.passwordDraft,
                    compliance = runtime.loginComplianceProvider.requestFor(profile),
                )
            }
            LoginEntryMode.New -> {
                val loginName = when (val result = SecondLifeLoginName.fromUserInput(state.usernameDraft)) {
                    is SecondLifeLoginNameResult.Valid -> result.loginName
                    is SecondLifeLoginNameResult.Invalid -> return invalidInput(result.reason.name)
                }
                val complianceProfile = SavedAccountProfile(
                    profileId = AccountProfileId("profile:v1:compliance-preview"),
                    loginName = loginName,
                    label = loginName.value,
                    credentialHandle = CredentialHandle("hostess-vault:v1:compliance-preview"),
                    startLocation = null,
                )
                authenticationService.loginNewProfile(
                    inputName = loginName.value,
                    passwordDraft = state.passwordDraft,
                    compliance = runtime.loginComplianceProvider.requestFor(complianceProfile),
                )
            }
        }

        return when (authentication) {
            is LoginProfileAuthenticationResult.Success -> copy(
                appState = appState.copy(
                    activeAccountLabel = authentication.profile.loginName.value,
                    session = authentication.session,
                ),
                authenticatedProfile = authentication.profile,
            )
            is LoginProfileAuthenticationResult.InvalidInput -> invalidInput(authentication.message)
            is LoginProfileAuthenticationResult.AuthenticationFailed -> authenticationFailure(authentication.message)
            is LoginProfileAuthenticationResult.CredentialStoreFailed -> credentialStoreFailure(authentication.message)
            is LoginProfileAuthenticationResult.NewProfileRollbackFailed -> rollbackFailure(authentication)
        }
    }

    fun completeAvatarReadiness(): LoginController {
        val profile = authenticatedProfile
            ?: return authenticationFailure(null)
        val session = appState.session
            ?: return authenticationFailure(null)
        return routeAfterReadiness(profile, session)
    }

    fun completeLogin(): LoginController =
        completeAuthentication().completeAvatarReadiness()

    fun finishLoginOperation(): LoginController =
        copy(state.copy(operation = LoginOperationUiState.Idle))

    private fun revealSavedPassword(profileId: AccountProfileId): LoginController {
        val credentialService = runtime.credentialServiceOrNull()
            ?: return credentialRuntimeUnavailable()
        val option = state.savedLoginOptions.firstOrNull { it.profileId == profileId }
        return when (val revealed = credentialService.revealPassword(profileId)) {
            is CredentialServiceRevealPasswordResult.Revealed -> copy(
                state.copy(
                    usernameDraft = revealed.profile.loginName.value,
                    selectedProfileId = profileId,
                    entryMode = LoginEntryMode.Saved(profileId),
                    passwordDraft = revealed.password,
                    passwordVisible = false,
                    passwordEnabled = true,
                    loginEnabled = true,
                    operation = state.operation.clearError(),
                ),
            )
            is CredentialServiceRevealPasswordResult.MissingProfile -> revealFailure(profileId, option?.loginName)
            is CredentialServiceRevealPasswordResult.ProfileStoreFailure -> {
                revealFailure(profileId, option?.loginName, revealed.message)
            }
            is CredentialServiceRevealPasswordResult.VaultFailure -> {
                revealFailure(profileId, option?.loginName, revealed.message)
            }
        }
    }

    private fun routeAfterReadiness(
        profile: SavedAccountProfile,
        session: HostessSession,
    ): LoginController =
        when (val readiness = runtime.avatarReadinessService.ensureReady(session)) {
            is AvatarReadinessResult.Success -> copy(
                state = state.afterSuccessfulLogin(),
                appState = appState.copy(
                    route = UiRoute.Compose,
                    menuOpen = false,
                    activeAccountLabel = profile.loginName.value,
                    sessionStrip = SessionStripUiState(
                        visible = true,
                        locationLabel = readiness.proof.regionName.orEmpty(),
                        statusKey = HostessTextKey.Online,
                        online = true,
                    ),
                    operationMessageKey = HostessTextKey.Ready,
                    session = session,
                ),
            )
            is AvatarReadinessResult.Failure -> authenticationFailure(readiness.failure.redactedMessage)
        }

    private fun LoginUiState.afterSuccessfulLogin(): LoginUiState =
        copy(
            usernameDraft = "",
            selectedProfileId = null,
            entryMode = LoginEntryMode.New,
            passwordDraft = "",
            passwordVisible = false,
            passwordEnabled = false,
            loginEnabled = false,
            operation = LoginOperationUiState.Idle,
        )

    private fun profileFor(profileId: AccountProfileId): SavedAccountProfile? =
        when (val listed = runtime.credentialServiceOrNull()?.listProfiles()) {
            is AccountProfileStoreListResult.Listed -> listed.profiles.firstOrNull { it.profileId == profileId }
            else -> null
        }

    private fun revealFailure(
        profileId: AccountProfileId,
        username: String?,
        message: String? = null,
    ): LoginController =
        copy(
            state.copy(
                usernameDraft = username.orEmpty(),
                selectedProfileId = profileId,
                entryMode = LoginEntryMode.Saved(profileId),
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                loginEnabled = false,
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.LoginFailed,
                    errorMessage = message,
                ),
            ),
        )

    private fun invalidInput(message: String?): LoginController =
        copy(
            state.copy(
                loginEnabled = false,
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.LoginFailed,
                    errorMessage = message,
                ),
            ),
        )

    private fun authenticationFailure(message: String?): LoginController =
        copy(
            state.copy(
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.LoginFailed,
                    errorMessage = message,
                ),
            ),
            appState = appState.afterLoginFailure(),
        )

    private fun credentialStoreFailure(message: String?): LoginController =
        copy(
            state.copy(
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.LoginFailed,
                    errorMessage = message,
                ),
            ),
            appState = appState.afterLoginFailure(),
        )

    private fun rollbackFailure(result: LoginProfileAuthenticationResult.NewProfileRollbackFailed): LoginController =
        copy(
            state.copy(
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.RemovingFailedLogin,
                    errorMessage = result.rollbackMessage ?: result.authenticationMessage,
                ),
            ),
        )

    private fun credentialRuntimeUnavailable(message: String? = state.credentialRuntime.message): LoginController =
        copy(
            state.copy(
                usernameDraft = "",
                selectedProfileId = null,
                entryMode = LoginEntryMode.New,
                passwordDraft = "",
                passwordVisible = false,
                passwordEnabled = false,
                loginEnabled = false,
                operation = LoginOperationUiState.error(
                    errorKey = HostessTextKey.LoginFailed,
                    errorMessage = message,
                ),
            ),
            appState = appState.afterLoginFailure(),
        )

    private fun copy(
        state: LoginUiState = this.state,
        appState: AppUiState = this.appState,
        authenticatedProfile: SavedAccountProfile? = this.authenticatedProfile,
    ): LoginController =
        LoginController(runtime, state, appState, authenticatedProfile)
}

private fun LoginOperationUiState.clearError(): LoginOperationUiState =
    copy(errorKey = null, errorMessage = null)

private fun LoginOperationUiState.Companion.inFlight(messageKey: HostessTextKey): LoginOperationUiState =
    LoginOperationUiState(
        inFlight = true,
        messageKey = messageKey,
        errorKey = null,
        errorMessage = null,
    )

private fun LoginOperationUiState.Companion.error(
    errorKey: HostessTextKey,
    errorMessage: String?,
): LoginOperationUiState =
    LoginOperationUiState(
        inFlight = false,
        messageKey = null,
        errorKey = errorKey,
        errorMessage = errorMessage,
    )

private fun AppUiState.afterLoginFailure(): AppUiState =
    copy(
        route = UiRoute.Login,
        menuOpen = false,
        activeAccountLabel = "",
        sessionStrip = SessionStripUiState(),
        operationMessageKey = null,
        session = null,
    )
