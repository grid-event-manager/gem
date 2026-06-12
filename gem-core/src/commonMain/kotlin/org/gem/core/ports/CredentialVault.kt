package org.gem.core.ports

import org.gem.core.domain.LoginCredentialMaterial

interface CredentialVault {
    fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult

    fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult

    fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult

    fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult
}

sealed interface CredentialVaultSaveResult {
    data class Saved(val credentialHandle: CredentialHandle) : CredentialVaultSaveResult
    data class KeySourceFailed(val message: String? = null) : CredentialVaultSaveResult
    data class CryptoFailed(val message: String? = null) : CredentialVaultSaveResult
    data class CorruptVault(val message: String? = null) : CredentialVaultSaveResult
    data class StorageFailed(val message: String? = null) : CredentialVaultSaveResult
}

sealed interface CredentialVaultUpdateResult {
    data class Updated(val credentialHandle: CredentialHandle) : CredentialVaultUpdateResult
    data class Missing(val credentialHandle: CredentialHandle, val message: String? = null) : CredentialVaultUpdateResult
    data class KeySourceFailed(val message: String? = null) : CredentialVaultUpdateResult
    data class CryptoFailed(val message: String? = null) : CredentialVaultUpdateResult
    data class CorruptVault(val message: String? = null) : CredentialVaultUpdateResult
    data class StorageFailed(val message: String? = null) : CredentialVaultUpdateResult
}

sealed interface CredentialVaultDeleteResult {
    data class Deleted(val credentialHandle: CredentialHandle) : CredentialVaultDeleteResult
    data class Missing(val credentialHandle: CredentialHandle, val message: String? = null) : CredentialVaultDeleteResult
    data class KeySourceFailed(val message: String? = null) : CredentialVaultDeleteResult
    data class CryptoFailed(val message: String? = null) : CredentialVaultDeleteResult
    data class CorruptVault(val message: String? = null) : CredentialVaultDeleteResult
    data class StorageFailed(val message: String? = null) : CredentialVaultDeleteResult
}

sealed interface CredentialVaultResolveResult {
    data class Resolved(val material: LoginCredentialMaterial) : CredentialVaultResolveResult
    data class Missing(val credentialHandle: CredentialHandle, val message: String? = null) : CredentialVaultResolveResult
    data class KeySourceFailed(val message: String? = null) : CredentialVaultResolveResult
    data class CryptoFailed(val message: String? = null) : CredentialVaultResolveResult
    data class CorruptVault(val message: String? = null) : CredentialVaultResolveResult
    data class StorageFailed(val message: String? = null) : CredentialVaultResolveResult
}
