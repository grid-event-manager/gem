package org.hostess.core.services

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult

class SavedLoginAuthenticationService(
    private val credentialService: CredentialService,
    private val sessionService: SessionService,
) {
    fun loginWithSavedProfile(
        profileId: AccountProfileId,
        passwordDraft: String,
        compliance: LoginComplianceRequest,
    ): SavedLoginAuthenticationResult {
        if (passwordDraft.isBlank()) {
            return SavedLoginAuthenticationResult.InvalidPasswordDraft
        }

        val revealed = when (val result = credentialService.revealPassword(profileId)) {
            is CredentialServiceRevealPasswordResult.Revealed -> result
            is CredentialServiceRevealPasswordResult.MissingProfile -> {
                return SavedLoginAuthenticationResult.MissingProfile(result.profileId)
            }
            is CredentialServiceRevealPasswordResult.ProfileStoreFailure -> {
                return SavedLoginAuthenticationResult.RevealFailed(result.message)
            }
            is CredentialServiceRevealPasswordResult.VaultFailure -> {
                return SavedLoginAuthenticationResult.RevealFailed(result.message)
            }
        }
        val previousPassword = revealed.password
        val passwordChanged = passwordDraft != previousPassword

        if (passwordChanged) {
            when (val update = credentialService.updatePassword(profileId, passwordDraft)) {
                CredentialServiceUpdatePasswordResult.Updated -> Unit
                is CredentialServiceUpdatePasswordResult.MissingProfile -> {
                    return SavedLoginAuthenticationResult.MissingProfile(update.profileId)
                }
                CredentialServiceUpdatePasswordResult.InvalidSecret -> {
                    return SavedLoginAuthenticationResult.InvalidPasswordDraft
                }
                is CredentialServiceUpdatePasswordResult.ProfileStoreFailure -> {
                    return SavedLoginAuthenticationResult.UpdateFailed(update.message)
                }
                is CredentialServiceUpdatePasswordResult.VaultFailure -> {
                    return SavedLoginAuthenticationResult.UpdateFailed(update.message)
                }
            }
        }

        val loginRequest = LoginRequest(
            accountLabel = AccountLabel(revealed.profile.label),
            credentialHandle = revealed.profile.credentialHandle,
        )

        return when (val login = sessionService.login(loginRequest, compliance)) {
            is SessionLoginResult.Success -> {
                SavedLoginAuthenticationResult.Success(revealed.profile, login.session)
            }
            is SessionLoginResult.Failure -> loginFailureResult(
                login.failure,
                profileId,
                previousPassword,
                passwordChanged,
            )
        }
    }

    private fun loginFailureResult(
        loginFailure: CoreFailure,
        profileId: AccountProfileId,
        previousPassword: String,
        passwordChanged: Boolean,
    ): SavedLoginAuthenticationResult {
        if (!passwordChanged) {
            return SavedLoginAuthenticationResult.LoginFailed(loginFailure)
        }

        return when (val restore = credentialService.updatePassword(profileId, previousPassword)) {
            CredentialServiceUpdatePasswordResult.Updated -> {
                SavedLoginAuthenticationResult.LoginFailed(loginFailure)
            }
            is CredentialServiceUpdatePasswordResult.MissingProfile -> {
                SavedLoginAuthenticationResult.RestoreFailed(loginFailure, "profile missing during restore")
            }
            CredentialServiceUpdatePasswordResult.InvalidSecret -> {
                SavedLoginAuthenticationResult.RestoreFailed(loginFailure, "prior password invalid during restore")
            }
            is CredentialServiceUpdatePasswordResult.ProfileStoreFailure -> {
                SavedLoginAuthenticationResult.RestoreFailed(loginFailure, restore.message)
            }
            is CredentialServiceUpdatePasswordResult.VaultFailure -> {
                SavedLoginAuthenticationResult.RestoreFailed(loginFailure, restore.message)
            }
        }
    }
}
