package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginNameInvalidReason

sealed interface CredentialServiceAddResult {
    data class Saved(val profile: SavedAccountProfile) : CredentialServiceAddResult
    data class InvalidLoginName(val reason: SecondLifeLoginNameInvalidReason) : CredentialServiceAddResult
    data object InvalidSecret : CredentialServiceAddResult
    data class VaultFailure(val message: String? = null) : CredentialServiceAddResult
    data class ProfileStoreFailure(val message: String? = null) : CredentialServiceAddResult
}

sealed interface CredentialServiceUpdatePasswordResult {
    data object Updated : CredentialServiceUpdatePasswordResult
    data class MissingProfile(val profileId: AccountProfileId) : CredentialServiceUpdatePasswordResult
    data object InvalidSecret : CredentialServiceUpdatePasswordResult
    data class VaultFailure(val message: String? = null) : CredentialServiceUpdatePasswordResult
    data class ProfileStoreFailure(val message: String? = null) : CredentialServiceUpdatePasswordResult
}

sealed interface CredentialServiceRevealPasswordResult {
    class Revealed(
        val profile: SavedAccountProfile,
        val password: String,
    ) : CredentialServiceRevealPasswordResult {
        override fun toString(): String =
            "CredentialServiceRevealPasswordResult.Revealed(profile=$profile, password=[redacted])"
    }

    data class MissingProfile(val profileId: AccountProfileId) : CredentialServiceRevealPasswordResult
    data class VaultFailure(val message: String? = null) : CredentialServiceRevealPasswordResult
    data class ProfileStoreFailure(val message: String? = null) : CredentialServiceRevealPasswordResult
}

sealed interface CredentialServiceDeleteResult {
    data class Deleted(val profileIds: Set<AccountProfileId>) : CredentialServiceDeleteResult
    data class MissingProfiles(val profileIds: Set<AccountProfileId>) : CredentialServiceDeleteResult
    data class VaultFailure(val message: String? = null) : CredentialServiceDeleteResult
    data class ProfileStoreFailure(val message: String? = null) : CredentialServiceDeleteResult
}
