package org.hostess.core.services

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SavedAccountProfile

sealed interface SavedLoginAuthenticationResult {
    data class Success(
        val profile: SavedAccountProfile,
        val session: HostessSession,
    ) : SavedLoginAuthenticationResult

    data class MissingProfile(val profileId: AccountProfileId) : SavedLoginAuthenticationResult
    data object InvalidPasswordDraft : SavedLoginAuthenticationResult
    data class RevealFailed(val message: String? = null) : SavedLoginAuthenticationResult
    data class UpdateFailed(val message: String? = null) : SavedLoginAuthenticationResult
    data class LoginFailed(val failure: CoreFailure) : SavedLoginAuthenticationResult
    data class RestoreFailed(
        val loginFailure: CoreFailure,
        val restoreMessage: String? = null,
    ) : SavedLoginAuthenticationResult
}
