package org.hostess.credential.vault

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.AccountProfileStoreDeleteResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AccountProfileStoreSaveResult
import org.hostess.core.ports.AccountProfileStoreUpdateResult

class VaultFileAccountProfileStore internal constructor(
    private val session: VaultDocumentSession,
) : AccountProfileStore {
    override fun list(): AccountProfileStoreListResult =
        when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success ->
                AccountProfileStoreListResult.Listed(current.value.profiles.sortedBy { it.profileId.value }.map { it.toProfile() })
            is VaultDocumentSessionResult.CorruptVault -> AccountProfileStoreListResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> AccountProfileStoreListResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> AccountProfileStoreListResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.KeySourceFailed -> AccountProfileStoreListResult.StorageFailed(current.message)
        }

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.CorruptVault -> return AccountProfileStoreSaveResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return AccountProfileStoreSaveResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return AccountProfileStoreSaveResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.KeySourceFailed -> return AccountProfileStoreSaveResult.StorageFailed(current.message)
        }
        if (snapshot.profiles.any { it.profileId == profile.profileId }) {
            return AccountProfileStoreSaveResult.StorageFailed("duplicate_profile_id")
        }
        if (snapshot.credentials.none { it.credentialHandle == profile.credentialHandle }) {
            return AccountProfileStoreSaveResult.CorruptVault("missing_profile_credential")
        }

        val next = snapshot.copy(profiles = snapshot.profiles + profile.toVaultProfileRecord())
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> AccountProfileStoreSaveResult.Saved(profile)
            is VaultDocumentSessionResult.CorruptVault -> AccountProfileStoreSaveResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> AccountProfileStoreSaveResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> AccountProfileStoreSaveResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.KeySourceFailed -> AccountProfileStoreSaveResult.StorageFailed(written.message)
        }
    }

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.CorruptVault -> return AccountProfileStoreUpdateResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return AccountProfileStoreUpdateResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return AccountProfileStoreUpdateResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.KeySourceFailed -> return AccountProfileStoreUpdateResult.StorageFailed(current.message)
        }
        if (snapshot.profiles.none { it.profileId == profile.profileId }) {
            return AccountProfileStoreUpdateResult.Missing(profile.profileId)
        }
        if (snapshot.credentials.none { it.credentialHandle == profile.credentialHandle }) {
            return AccountProfileStoreUpdateResult.CorruptVault("missing_profile_credential")
        }

        val next = snapshot.copy(
            profiles = snapshot.profiles.map { if (it.profileId == profile.profileId) profile.toVaultProfileRecord() else it },
        )
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> AccountProfileStoreUpdateResult.Updated(profile)
            is VaultDocumentSessionResult.CorruptVault -> AccountProfileStoreUpdateResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> AccountProfileStoreUpdateResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> AccountProfileStoreUpdateResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.KeySourceFailed -> AccountProfileStoreUpdateResult.StorageFailed(written.message)
        }
    }

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.CorruptVault -> return AccountProfileStoreDeleteResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return AccountProfileStoreDeleteResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return AccountProfileStoreDeleteResult.StorageFailed(current.message)
            is VaultDocumentSessionResult.KeySourceFailed -> return AccountProfileStoreDeleteResult.StorageFailed(current.message)
        }
        if (snapshot.profiles.none { it.profileId == profileId }) {
            return AccountProfileStoreDeleteResult.Missing(profileId)
        }

        val profiles = snapshot.profiles.filterNot { it.profileId == profileId }
        val credentials = session.pruneDeferredCredentialDeletes(profiles, snapshot.credentials)
        val next = snapshot.copy(profiles = profiles, credentials = credentials)
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> AccountProfileStoreDeleteResult.Deleted(profileId)
            is VaultDocumentSessionResult.CorruptVault -> AccountProfileStoreDeleteResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> AccountProfileStoreDeleteResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> AccountProfileStoreDeleteResult.StorageFailed(written.message)
            is VaultDocumentSessionResult.KeySourceFailed -> AccountProfileStoreDeleteResult.StorageFailed(written.message)
        }
    }

    private fun HostessVaultProfileRecord.toProfile(): SavedAccountProfile =
        SavedAccountProfile(profileId, loginName, label, credentialHandle, startLocation)

    private fun SavedAccountProfile.toVaultProfileRecord(): HostessVaultProfileRecord =
        HostessVaultProfileRecord(profileId, loginName, label, credentialHandle, startLocation)
}
