package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginNameInvalidReason

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
