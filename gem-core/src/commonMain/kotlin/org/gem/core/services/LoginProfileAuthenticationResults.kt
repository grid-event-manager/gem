package org.gem.core.services

import org.gem.core.domain.GemSession
import org.gem.core.domain.SavedAccountProfile

sealed interface LoginProfileAuthenticationResult {
    data class Success(
        val profile: SavedAccountProfile,
        val session: GemSession,
    ) : LoginProfileAuthenticationResult

    data class InvalidInput(val message: String? = null) : LoginProfileAuthenticationResult
    data class AuthenticationFailed(val message: String? = null) : LoginProfileAuthenticationResult
    data class CredentialStoreFailed(val message: String? = null) : LoginProfileAuthenticationResult
    data class NewProfileRollbackFailed(
        val authenticationMessage: String? = null,
        val rollbackMessage: String? = null,
    ) : LoginProfileAuthenticationResult
}
