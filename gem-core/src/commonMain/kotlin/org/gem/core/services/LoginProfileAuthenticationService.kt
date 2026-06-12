package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.LoginComplianceRequest

class LoginProfileAuthenticationService(
    private val credentialService: CredentialService,
    private val savedLoginAuthenticationService: SavedLoginAuthenticationService,
) {
    fun loginSavedProfile(
        profileId: AccountProfileId,
        passwordDraft: String,
        compliance: LoginComplianceRequest,
    ): LoginProfileAuthenticationResult =
        savedLoginAuthenticationService
            .loginWithSavedProfile(profileId, passwordDraft, compliance)
            .toProfileAuthenticationResult()

    fun loginNewProfile(
        inputName: String,
        passwordDraft: String,
        compliance: LoginComplianceRequest,
    ): LoginProfileAuthenticationResult =
        when (val added = credentialService.addLogin(inputName, passwordDraft)) {
            is CredentialServiceAddResult.Saved -> authenticateNewProfile(added, passwordDraft, compliance)
            is CredentialServiceAddResult.InvalidLoginName -> {
                LoginProfileAuthenticationResult.InvalidInput(added.reason.name)
            }
            CredentialServiceAddResult.InvalidSecret -> {
                LoginProfileAuthenticationResult.InvalidInput("invalid secret")
            }
            is CredentialServiceAddResult.ProfileStoreFailure -> {
                LoginProfileAuthenticationResult.CredentialStoreFailed(added.message)
            }
            is CredentialServiceAddResult.VaultFailure -> {
                LoginProfileAuthenticationResult.CredentialStoreFailed(added.message)
            }
        }

    private fun authenticateNewProfile(
        added: CredentialServiceAddResult.Saved,
        passwordDraft: String,
        compliance: LoginComplianceRequest,
    ): LoginProfileAuthenticationResult {
        val authentication = savedLoginAuthenticationService
            .loginWithSavedProfile(added.profile.profileId, passwordDraft, compliance)
            .toProfileAuthenticationResult()
        if (authentication is LoginProfileAuthenticationResult.Success) {
            return authentication
        }

        val authenticationMessage = authentication.visibleMessage()
        return when (val rollback = credentialService.deleteProfiles(setOf(added.profile.profileId))) {
            is CredentialServiceDeleteResult.Deleted,
            is CredentialServiceDeleteResult.MissingProfiles,
            -> authentication
            is CredentialServiceDeleteResult.ProfileStoreFailure -> {
                LoginProfileAuthenticationResult.NewProfileRollbackFailed(
                    authenticationMessage = authenticationMessage,
                    rollbackMessage = rollback.message,
                )
            }
            is CredentialServiceDeleteResult.VaultFailure -> {
                LoginProfileAuthenticationResult.NewProfileRollbackFailed(
                    authenticationMessage = authenticationMessage,
                    rollbackMessage = rollback.message,
                )
            }
        }
    }

    private fun SavedLoginAuthenticationResult.toProfileAuthenticationResult(): LoginProfileAuthenticationResult =
        when (this) {
            is SavedLoginAuthenticationResult.Success -> {
                LoginProfileAuthenticationResult.Success(profile, session)
            }
            is SavedLoginAuthenticationResult.MissingProfile -> {
                LoginProfileAuthenticationResult.CredentialStoreFailed("missing profile")
            }
            SavedLoginAuthenticationResult.InvalidPasswordDraft -> {
                LoginProfileAuthenticationResult.InvalidInput("invalid password")
            }
            is SavedLoginAuthenticationResult.RevealFailed -> {
                LoginProfileAuthenticationResult.CredentialStoreFailed(message)
            }
            is SavedLoginAuthenticationResult.UpdateFailed -> {
                LoginProfileAuthenticationResult.CredentialStoreFailed(message)
            }
            is SavedLoginAuthenticationResult.LoginFailed -> {
                LoginProfileAuthenticationResult.AuthenticationFailed(failure.redactedMessage)
            }
            is SavedLoginAuthenticationResult.RestoreFailed -> {
                LoginProfileAuthenticationResult.AuthenticationFailed(restoreMessage ?: loginFailure.redactedMessage)
            }
        }

    private fun LoginProfileAuthenticationResult.visibleMessage(): String? =
        when (this) {
            is LoginProfileAuthenticationResult.AuthenticationFailed -> message
            is LoginProfileAuthenticationResult.CredentialStoreFailed -> message
            is LoginProfileAuthenticationResult.InvalidInput -> message
            is LoginProfileAuthenticationResult.NewProfileRollbackFailed -> rollbackMessage ?: authenticationMessage
            is LoginProfileAuthenticationResult.Success -> null
        }
}
