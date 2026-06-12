package org.gem.core.services

import org.gem.core.domain.AccountProfileId

class SavedAccountManagementService(
    private val credentialService: CredentialService,
) {
    fun addAccount(
        inputName: String,
        passwordDraft: String,
    ): SavedAccountAddResult =
        when (val added = credentialService.addLogin(inputName, passwordDraft)) {
            is CredentialServiceAddResult.Saved -> SavedAccountAddResult.Saved(added.profile)
            is CredentialServiceAddResult.InvalidLoginName -> SavedAccountAddResult.InvalidLoginName(added.reason)
            CredentialServiceAddResult.InvalidSecret -> SavedAccountAddResult.InvalidSecret
            is CredentialServiceAddResult.ProfileStoreFailure -> SavedAccountAddResult.CredentialStoreFailed(added.message)
            is CredentialServiceAddResult.VaultFailure -> SavedAccountAddResult.CredentialStoreFailed(added.message)
        }

    fun deleteAccounts(profileIds: Set<AccountProfileId>): SavedAccountDeleteResult =
        when (val deleted = credentialService.deleteProfiles(profileIds)) {
            is CredentialServiceDeleteResult.Deleted -> SavedAccountDeleteResult.Deleted(deleted.profileIds)
            is CredentialServiceDeleteResult.MissingProfiles -> SavedAccountDeleteResult.MissingProfiles(deleted.profileIds)
            is CredentialServiceDeleteResult.ProfileStoreFailure -> {
                SavedAccountDeleteResult.CredentialStoreFailed(deleted.message)
            }
            is CredentialServiceDeleteResult.VaultFailure -> SavedAccountDeleteResult.CredentialStoreFailed(deleted.message)
        }
}
