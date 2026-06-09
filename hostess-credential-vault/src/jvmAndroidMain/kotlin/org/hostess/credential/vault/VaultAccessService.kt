package org.hostess.credential.vault

import java.security.SecureRandom
import org.hostess.core.ports.AccountProfileIdSource
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.CredentialVault

class VaultAccessService(
    private val keySource: VaultKeySource,
    private val fileStore: HostessVaultFileStore,
    private val cipher: JcaVaultCipher = JcaVaultCipher(),
    private val accountProfileIdSource: AccountProfileIdSource = SecureRandomAccountProfileIdSource(),
    private val credentialHandleRandom: SecureRandom = SecureRandom(),
) {
    private var activeSession: VaultDocumentSession? = null

    fun open(): VaultAccessOpenResult {
        val key = when (val keyResult = keySource.getOrCreateKey()) {
            is VaultKeySourceResult.Loaded -> keyResult.keyMaterial
            is VaultKeySourceResult.KeySourceFailed -> return VaultAccessOpenResult.KeySourceFailed(keyResult.message)
        }

        val session = when (val opened = VaultDocumentSession.open(fileStore, cipher, key)) {
            is VaultDocumentOpenResult.Opened -> opened.session
            is VaultDocumentOpenResult.CryptoFailed -> return VaultAccessOpenResult.CryptoFailed(opened.message)
            is VaultDocumentOpenResult.CorruptVault -> return VaultAccessOpenResult.CorruptVault(opened.message)
            is VaultDocumentOpenResult.StorageFailed -> return VaultAccessOpenResult.StorageFailed(opened.message)
        }

        activeSession = session
        return VaultAccessOpenResult.Ready(
            credentialVault = EncryptedHostessVault(session, credentialHandleRandom),
            accountProfileStore = VaultFileAccountProfileStore(session),
            accountProfileIdSource = accountProfileIdSource,
        )
    }

    fun lock() {
        activeSession?.lock()
        activeSession = null
    }

    fun reset(confirmed: Boolean): VaultAccessResetResult {
        if (!confirmed) {
            return VaultAccessResetResult.StorageFailed("reset_not_confirmed")
        }
        lock()
        return when (val vaultDelete = fileStore.delete()) {
            is HostessVaultFileDeleteResult.StorageFailed -> VaultAccessResetResult.StorageFailed(vaultDelete.message)
            HostessVaultFileDeleteResult.Deleted,
            HostessVaultFileDeleteResult.Missing,
            -> when (val keyDelete = keySource.deleteKey()) {
                VaultKeySourceDeleteResult.Deleted,
                VaultKeySourceDeleteResult.Missing,
                -> VaultAccessResetResult.ResetComplete
                is VaultKeySourceDeleteResult.KeySourceFailed -> VaultAccessResetResult.KeySourceFailed(keyDelete.message)
            }
        }
    }
}

sealed interface VaultAccessOpenResult {
    data class Ready(
        val credentialVault: CredentialVault,
        val accountProfileStore: AccountProfileStore,
        val accountProfileIdSource: AccountProfileIdSource,
    ) : VaultAccessOpenResult

    data class KeySourceFailed(val message: String? = null) : VaultAccessOpenResult
    data class CryptoFailed(val message: String? = null) : VaultAccessOpenResult
    data class CorruptVault(val message: String? = null) : VaultAccessOpenResult
    data class StorageFailed(val message: String? = null) : VaultAccessOpenResult
}

sealed interface VaultAccessResetResult {
    data object ResetComplete : VaultAccessResetResult
    data class KeySourceFailed(val message: String? = null) : VaultAccessResetResult
    data class StorageFailed(val message: String? = null) : VaultAccessResetResult
}

internal class VaultDocumentSession private constructor(
    private val fileStore: HostessVaultFileStore,
    private val cipher: JcaVaultCipher,
    keyMaterial: VaultKeyMaterial,
    plaintext: HostessVaultPlaintext,
) {
    private var keyMaterial: VaultKeyMaterial? = keyMaterial
    private var plaintext: HostessVaultPlaintext? = plaintext
    private val deferredCredentialDeletes: MutableSet<CredentialHandle> = mutableSetOf()

    fun snapshot(): VaultDocumentSessionResult<HostessVaultPlaintext> {
        val current = plaintext ?: return VaultDocumentSessionResult.KeySourceFailed("vault_locked")
        return VaultDocumentSessionResult.Success(current)
    }

    fun replace(next: HostessVaultPlaintext): VaultDocumentSessionResult<Unit> {
        val persisted = persist(next)
        if (persisted is VaultDocumentSessionResult.Success) {
            plaintext = next
        }
        return persisted
    }

    fun lock() {
        plaintext = null
        keyMaterial = null
        deferredCredentialDeletes.clear()
    }

    fun deferCredentialDelete(credentialHandle: CredentialHandle) {
        deferredCredentialDeletes += credentialHandle
    }

    fun isCredentialDeleteDeferred(credentialHandle: CredentialHandle): Boolean =
        credentialHandle in deferredCredentialDeletes

    fun pruneDeferredCredentialDeletes(
        profiles: List<HostessVaultProfileRecord>,
        credentials: List<HostessVaultCredentialRecord>,
    ): List<HostessVaultCredentialRecord> {
        val referencedHandles = profiles.map { it.credentialHandle }.toSet()
        val removable = deferredCredentialDeletes.filter { it !in referencedHandles }.toSet()
        deferredCredentialDeletes.removeAll(removable)
        return credentials.filterNot { it.credentialHandle in removable }
    }

    private fun persist(next: HostessVaultPlaintext): VaultDocumentSessionResult<Unit> {
        val key = keyMaterial ?: return VaultDocumentSessionResult.KeySourceFailed("vault_locked")
        val payload = try {
            HostessVaultFileCodec.encode(next)
        } catch (_: IllegalArgumentException) {
            return VaultDocumentSessionResult.CorruptVault("corrupt")
        }
        val envelope = when (val encrypted = cipher.encrypt(payload, key)) {
            is JcaVaultCipherEncryptResult.Encrypted -> encrypted.bytes
            is JcaVaultCipherEncryptResult.CryptoFailed -> return VaultDocumentSessionResult.CryptoFailed(encrypted.message)
            is JcaVaultCipherEncryptResult.CorruptVault -> return VaultDocumentSessionResult.CorruptVault(encrypted.message)
        }
        return when (val written = fileStore.writeAtomic(envelope)) {
            is HostessVaultFileWriteResult.Written -> VaultDocumentSessionResult.Success(Unit)
            is HostessVaultFileWriteResult.StorageFailed -> VaultDocumentSessionResult.StorageFailed(written.message)
        }
    }

    companion object {
        fun open(
            fileStore: HostessVaultFileStore,
            cipher: JcaVaultCipher,
            keyMaterial: VaultKeyMaterial,
        ): VaultDocumentOpenResult =
            when (val read = fileStore.read()) {
                HostessVaultFileReadResult.Missing -> createEmpty(fileStore, cipher, keyMaterial)
                is HostessVaultFileReadResult.Read -> decryptExisting(fileStore, cipher, keyMaterial, read.bytes)
                is HostessVaultFileReadResult.StorageFailed -> VaultDocumentOpenResult.StorageFailed(read.message)
            }

        private fun createEmpty(
            fileStore: HostessVaultFileStore,
            cipher: JcaVaultCipher,
            keyMaterial: VaultKeyMaterial,
        ): VaultDocumentOpenResult {
            val empty = HostessVaultPlaintext(profiles = emptyList(), credentials = emptyList())
            val session = VaultDocumentSession(fileStore, cipher, keyMaterial, empty)
            return when (val persisted = session.persist(empty)) {
                is VaultDocumentSessionResult.Success -> VaultDocumentOpenResult.Opened(session)
                is VaultDocumentSessionResult.CryptoFailed -> VaultDocumentOpenResult.CryptoFailed(persisted.message)
                is VaultDocumentSessionResult.CorruptVault -> VaultDocumentOpenResult.CorruptVault(persisted.message)
                is VaultDocumentSessionResult.StorageFailed -> VaultDocumentOpenResult.StorageFailed(persisted.message)
                is VaultDocumentSessionResult.KeySourceFailed -> VaultDocumentOpenResult.StorageFailed(persisted.message)
            }
        }

        private fun decryptExisting(
            fileStore: HostessVaultFileStore,
            cipher: JcaVaultCipher,
            keyMaterial: VaultKeyMaterial,
            bytes: ByteArray,
        ): VaultDocumentOpenResult {
            val payload = when (val decrypted = cipher.decrypt(bytes, keyMaterial)) {
                is JcaVaultCipherDecryptResult.Decrypted -> decrypted.plaintext
                is JcaVaultCipherDecryptResult.CryptoFailed -> return VaultDocumentOpenResult.CryptoFailed(decrypted.message)
                is JcaVaultCipherDecryptResult.CorruptVault -> return VaultDocumentOpenResult.CorruptVault(decrypted.message)
            }
            val plaintext = when (val decoded = HostessVaultFileCodec.decode(payload)) {
                is HostessVaultCodecResult.Decoded -> decoded.plaintext
                is HostessVaultCodecResult.Corrupt -> return VaultDocumentOpenResult.CorruptVault(decoded.message)
            }
            return VaultDocumentOpenResult.Opened(VaultDocumentSession(fileStore, cipher, keyMaterial, plaintext))
        }
    }
}

internal sealed interface VaultDocumentOpenResult {
    data class Opened(val session: VaultDocumentSession) : VaultDocumentOpenResult
    data class CryptoFailed(val message: String? = null) : VaultDocumentOpenResult
    data class CorruptVault(val message: String? = null) : VaultDocumentOpenResult
    data class StorageFailed(val message: String? = null) : VaultDocumentOpenResult
}

internal sealed interface VaultDocumentSessionResult<out T> {
    data class Success<T>(val value: T) : VaultDocumentSessionResult<T>
    data class KeySourceFailed(val message: String? = null) : VaultDocumentSessionResult<Nothing>
    data class CryptoFailed(val message: String? = null) : VaultDocumentSessionResult<Nothing>
    data class CorruptVault(val message: String? = null) : VaultDocumentSessionResult<Nothing>
    data class StorageFailed(val message: String? = null) : VaultDocumentSessionResult<Nothing>
}
