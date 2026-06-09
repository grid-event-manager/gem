package org.hostess.core.services

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import org.hostess.core.ports.AccountProfileIdSource
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.AccountProfileStoreDeleteResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AccountProfileStoreSaveResult
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultDeleteResult
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult
import org.hostess.core.ports.CredentialVaultUpdateResult

class CredentialService(
    private val accountProfileStore: AccountProfileStore,
    private val credentialVault: CredentialVault,
    private val accountProfileIdSource: AccountProfileIdSource,
) {
    fun listProfiles(): AccountProfileStoreListResult =
        accountProfileStore.list()

    fun addLogin(
        inputName: String,
        password: String,
        loginUri: SecondLifeLoginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
        startLocation: String? = null,
    ): CredentialServiceAddResult {
        val loginName = when (val result = org.hostess.core.domain.SecondLifeLoginName.fromUserInput(inputName)) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> return CredentialServiceAddResult.InvalidLoginName(result.reason)
        }
        val sharedSecret = SharedSecret.fromPlainText(password)
            ?: return CredentialServiceAddResult.InvalidSecret
        val material = LoginCredentialMaterial(loginUri, loginName, sharedSecret, startLocation)

        return when (val savedCredential = credentialVault.save(material)) {
            is CredentialVaultSaveResult.Saved -> saveProfile(loginName.value, loginName, savedCredential, startLocation)
            is CredentialVaultSaveResult.KeySourceFailed -> CredentialServiceAddResult.VaultFailure(savedCredential.message)
            is CredentialVaultSaveResult.CryptoFailed -> CredentialServiceAddResult.VaultFailure(savedCredential.message)
            is CredentialVaultSaveResult.CorruptVault -> CredentialServiceAddResult.VaultFailure(savedCredential.message)
            is CredentialVaultSaveResult.StorageFailed -> CredentialServiceAddResult.VaultFailure(savedCredential.message)
        }
    }

    fun updatePassword(
        profileId: AccountProfileId,
        password: String,
    ): CredentialServiceUpdatePasswordResult {
        val profile = when (val listed = accountProfileStore.list()) {
            is AccountProfileStoreListResult.Listed ->
                listed.profiles.firstOrNull { it.profileId == profileId }
                    ?: return CredentialServiceUpdatePasswordResult.MissingProfile(profileId)
            is AccountProfileStoreListResult.CorruptVault -> {
                return CredentialServiceUpdatePasswordResult.ProfileStoreFailure(listed.message)
            }
            is AccountProfileStoreListResult.StorageFailed -> {
                return CredentialServiceUpdatePasswordResult.ProfileStoreFailure(listed.message)
            }
        }
        val newSecret = SharedSecret.fromPlainText(password)
            ?: return CredentialServiceUpdatePasswordResult.InvalidSecret

        val existingMaterial = when (val resolved = credentialVault.resolve(profile.credentialHandle)) {
            is CredentialVaultResolveResult.Resolved -> resolved.material
            is CredentialVaultResolveResult.Missing -> return CredentialServiceUpdatePasswordResult.VaultFailure(resolved.message)
            is CredentialVaultResolveResult.KeySourceFailed -> {
                return CredentialServiceUpdatePasswordResult.VaultFailure(resolved.message)
            }
            is CredentialVaultResolveResult.CryptoFailed -> return CredentialServiceUpdatePasswordResult.VaultFailure(resolved.message)
            is CredentialVaultResolveResult.CorruptVault -> return CredentialServiceUpdatePasswordResult.VaultFailure(resolved.message)
            is CredentialVaultResolveResult.StorageFailed -> return CredentialServiceUpdatePasswordResult.VaultFailure(resolved.message)
        }

        val replacement = LoginCredentialMaterial(
            loginUri = existingMaterial.loginUri,
            loginName = existingMaterial.loginName,
            sharedSecret = newSecret,
            startLocation = existingMaterial.startLocation,
        )

        return when (val updated = credentialVault.update(profile.credentialHandle, replacement)) {
            is CredentialVaultUpdateResult.Updated -> CredentialServiceUpdatePasswordResult.Updated
            is CredentialVaultUpdateResult.Missing -> CredentialServiceUpdatePasswordResult.VaultFailure(updated.message)
            is CredentialVaultUpdateResult.KeySourceFailed -> CredentialServiceUpdatePasswordResult.VaultFailure(updated.message)
            is CredentialVaultUpdateResult.CryptoFailed -> CredentialServiceUpdatePasswordResult.VaultFailure(updated.message)
            is CredentialVaultUpdateResult.CorruptVault -> CredentialServiceUpdatePasswordResult.VaultFailure(updated.message)
            is CredentialVaultUpdateResult.StorageFailed -> CredentialServiceUpdatePasswordResult.VaultFailure(updated.message)
        }
    }

    fun deleteProfiles(profileIds: Set<AccountProfileId>): CredentialServiceDeleteResult {
        val profiles = when (val listed = accountProfileStore.list()) {
            is AccountProfileStoreListResult.Listed -> listed.profiles
            is AccountProfileStoreListResult.CorruptVault -> return CredentialServiceDeleteResult.ProfileStoreFailure(listed.message)
            is AccountProfileStoreListResult.StorageFailed -> return CredentialServiceDeleteResult.ProfileStoreFailure(listed.message)
        }
        val profilesById = profiles.associateBy { it.profileId }
        val missing = profileIds.filterNot(profilesById::containsKey).toSet()
        if (missing.isNotEmpty()) {
            return CredentialServiceDeleteResult.MissingProfiles(missing)
        }

        val deleted = mutableSetOf<AccountProfileId>()
        profileIds.sortedBy { it.value }.forEach { profileId ->
            val profile = profilesById.getValue(profileId)
            when (val vaultDelete = credentialVault.delete(profile.credentialHandle)) {
                is CredentialVaultDeleteResult.Deleted,
                is CredentialVaultDeleteResult.Missing,
                -> deleteProfile(profileId, deleted)?.let { return it }
                is CredentialVaultDeleteResult.KeySourceFailed -> {
                    return CredentialServiceDeleteResult.VaultFailure(vaultDelete.message)
                }
                is CredentialVaultDeleteResult.CryptoFailed -> return CredentialServiceDeleteResult.VaultFailure(vaultDelete.message)
                is CredentialVaultDeleteResult.CorruptVault -> return CredentialServiceDeleteResult.VaultFailure(vaultDelete.message)
                is CredentialVaultDeleteResult.StorageFailed -> return CredentialServiceDeleteResult.VaultFailure(vaultDelete.message)
            }
        }

        return CredentialServiceDeleteResult.Deleted(deleted.toSet())
    }

    private fun saveProfile(
        label: String,
        loginName: SecondLifeLoginName,
        savedCredential: CredentialVaultSaveResult.Saved,
        startLocation: String?,
    ): CredentialServiceAddResult {
        val profile = SavedAccountProfile(
            profileId = accountProfileIdSource.nextProfileId(),
            loginName = loginName,
            label = label,
            credentialHandle = savedCredential.credentialHandle,
            startLocation = startLocation,
        )

        return when (val savedProfile = accountProfileStore.save(profile)) {
            is AccountProfileStoreSaveResult.Saved -> CredentialServiceAddResult.Saved(savedProfile.profile)
            is AccountProfileStoreSaveResult.CorruptVault -> {
                credentialVault.delete(savedCredential.credentialHandle)
                CredentialServiceAddResult.ProfileStoreFailure(savedProfile.message)
            }
            is AccountProfileStoreSaveResult.StorageFailed -> {
                credentialVault.delete(savedCredential.credentialHandle)
                CredentialServiceAddResult.ProfileStoreFailure(savedProfile.message)
            }
        }
    }

    private fun deleteProfile(
        profileId: AccountProfileId,
        deleted: MutableSet<AccountProfileId>,
    ): CredentialServiceDeleteResult? =
        when (val profileDelete = accountProfileStore.delete(profileId)) {
            is AccountProfileStoreDeleteResult.Deleted -> {
                deleted += profileDelete.profileId
                null
            }
            is AccountProfileStoreDeleteResult.Missing -> {
                CredentialServiceDeleteResult.ProfileStoreFailure(profileDelete.message)
            }
            is AccountProfileStoreDeleteResult.CorruptVault -> {
                CredentialServiceDeleteResult.ProfileStoreFailure(profileDelete.message)
            }
            is AccountProfileStoreDeleteResult.StorageFailed -> {
                CredentialServiceDeleteResult.ProfileStoreFailure(profileDelete.message)
            }
        }
}
