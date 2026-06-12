package org.hostess.core.services

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginNameInvalidReason

sealed interface SavedAccountAddResult {
    data class Saved(val profile: SavedAccountProfile) : SavedAccountAddResult
    data class InvalidLoginName(val reason: SecondLifeLoginNameInvalidReason) : SavedAccountAddResult
    data object InvalidSecret : SavedAccountAddResult
    data class CredentialStoreFailed(val message: String? = null) : SavedAccountAddResult
}

sealed interface SavedAccountDeleteResult {
    data class Deleted(val profileIds: Set<AccountProfileId>) : SavedAccountDeleteResult
    data class MissingProfiles(val profileIds: Set<AccountProfileId>) : SavedAccountDeleteResult
    data class CredentialStoreFailed(val message: String? = null) : SavedAccountDeleteResult
}
