package org.hostess.credential.vault

import java.security.SecureRandom
import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultDeleteResult
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult
import org.hostess.core.ports.CredentialVaultUpdateResult

class EncryptedHostessVault internal constructor(
    private val session: VaultDocumentSession,
    private val secureRandom: SecureRandom = SecureRandom(),
) : CredentialVault {
    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.KeySourceFailed -> return CredentialVaultSaveResult.KeySourceFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return CredentialVaultSaveResult.CryptoFailed(current.message)
            is VaultDocumentSessionResult.CorruptVault -> return CredentialVaultSaveResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return CredentialVaultSaveResult.StorageFailed(current.message)
        }

        val credentialHandle = uniqueCredentialHandle(snapshot.credentials.map { it.credentialHandle }.toSet())
            ?: return CredentialVaultSaveResult.StorageFailed("handle_collision_exhausted")
        val next = snapshot.copy(
            credentials = snapshot.credentials + material.toVaultCredentialRecord(credentialHandle),
        )
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> CredentialVaultSaveResult.Saved(credentialHandle)
            is VaultDocumentSessionResult.KeySourceFailed -> CredentialVaultSaveResult.KeySourceFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> CredentialVaultSaveResult.CryptoFailed(written.message)
            is VaultDocumentSessionResult.CorruptVault -> CredentialVaultSaveResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> CredentialVaultSaveResult.StorageFailed(written.message)
        }
    }

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.KeySourceFailed -> return CredentialVaultUpdateResult.KeySourceFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return CredentialVaultUpdateResult.CryptoFailed(current.message)
            is VaultDocumentSessionResult.CorruptVault -> return CredentialVaultUpdateResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return CredentialVaultUpdateResult.StorageFailed(current.message)
        }
        if (snapshot.credentials.none { it.credentialHandle == credentialHandle }) {
            return CredentialVaultUpdateResult.Missing(credentialHandle)
        }
        if (session.isCredentialDeleteDeferred(credentialHandle)) {
            return CredentialVaultUpdateResult.Missing(credentialHandle)
        }

        val next = snapshot.copy(
            credentials = snapshot.credentials.map { credential ->
                if (credential.credentialHandle == credentialHandle) {
                    material.toVaultCredentialRecord(credentialHandle)
                } else {
                    credential
                }
            },
        )
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> CredentialVaultUpdateResult.Updated(credentialHandle)
            is VaultDocumentSessionResult.KeySourceFailed -> CredentialVaultUpdateResult.KeySourceFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> CredentialVaultUpdateResult.CryptoFailed(written.message)
            is VaultDocumentSessionResult.CorruptVault -> CredentialVaultUpdateResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> CredentialVaultUpdateResult.StorageFailed(written.message)
        }
    }

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.KeySourceFailed -> return CredentialVaultDeleteResult.KeySourceFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return CredentialVaultDeleteResult.CryptoFailed(current.message)
            is VaultDocumentSessionResult.CorruptVault -> return CredentialVaultDeleteResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return CredentialVaultDeleteResult.StorageFailed(current.message)
        }
        if (snapshot.credentials.none { it.credentialHandle == credentialHandle }) {
            return CredentialVaultDeleteResult.Missing(credentialHandle)
        }
        if (session.isCredentialDeleteDeferred(credentialHandle)) {
            return CredentialVaultDeleteResult.Deleted(credentialHandle)
        }
        if (snapshot.profiles.any { it.credentialHandle == credentialHandle }) {
            session.deferCredentialDelete(credentialHandle)
            return CredentialVaultDeleteResult.Deleted(credentialHandle)
        }

        val next = snapshot.copy(
            credentials = snapshot.credentials.filterNot { it.credentialHandle == credentialHandle },
        )
        return when (val written = session.replace(next)) {
            is VaultDocumentSessionResult.Success -> CredentialVaultDeleteResult.Deleted(credentialHandle)
            is VaultDocumentSessionResult.KeySourceFailed -> CredentialVaultDeleteResult.KeySourceFailed(written.message)
            is VaultDocumentSessionResult.CryptoFailed -> CredentialVaultDeleteResult.CryptoFailed(written.message)
            is VaultDocumentSessionResult.CorruptVault -> CredentialVaultDeleteResult.CorruptVault(written.message)
            is VaultDocumentSessionResult.StorageFailed -> CredentialVaultDeleteResult.StorageFailed(written.message)
        }
    }

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult {
        val snapshot = when (val current = session.snapshot()) {
            is VaultDocumentSessionResult.Success -> current.value
            is VaultDocumentSessionResult.KeySourceFailed -> return CredentialVaultResolveResult.KeySourceFailed(current.message)
            is VaultDocumentSessionResult.CryptoFailed -> return CredentialVaultResolveResult.CryptoFailed(current.message)
            is VaultDocumentSessionResult.CorruptVault -> return CredentialVaultResolveResult.CorruptVault(current.message)
            is VaultDocumentSessionResult.StorageFailed -> return CredentialVaultResolveResult.StorageFailed(current.message)
        }
        if (session.isCredentialDeleteDeferred(credentialHandle)) {
            return CredentialVaultResolveResult.Missing(credentialHandle)
        }
        val credential = snapshot.credentials.firstOrNull { it.credentialHandle == credentialHandle }
            ?: return CredentialVaultResolveResult.Missing(credentialHandle)
        return CredentialVaultResolveResult.Resolved(
            LoginCredentialMaterial(
                loginUri = credential.loginUri,
                loginName = credential.loginName,
                sharedSecret = credential.sharedSecret,
                startLocation = credential.startLocation,
            ),
        )
    }

    private fun uniqueCredentialHandle(existing: Set<CredentialHandle>): CredentialHandle? {
        repeat(HANDLE_COLLISION_RETRIES) {
            val handle = CredentialHandle(
                "${CredentialHandle.HOSTESS_VAULT_CREDENTIAL_PREFIX}${VaultRandomId.nextBase64UrlId(secureRandom)}",
            )
            if (handle !in existing) {
                return handle
            }
        }
        return null
    }

    private fun LoginCredentialMaterial.toVaultCredentialRecord(
        credentialHandle: CredentialHandle,
    ): HostessVaultCredentialRecord =
        HostessVaultCredentialRecord(
            credentialHandle = credentialHandle,
            loginUri = loginUri,
            loginName = loginName,
            sharedSecret = sharedSecret,
            startLocation = startLocation,
        )

    private companion object {
        const val HANDLE_COLLISION_RETRIES: Int = 3
    }
}
