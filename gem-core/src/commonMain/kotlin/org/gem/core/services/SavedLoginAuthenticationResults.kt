package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.GemSession
import org.gem.core.domain.SavedAccountProfile

sealed interface SavedLoginAuthenticationResult {
    data class Success(
        val profile: SavedAccountProfile,
        val session: GemSession,
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
