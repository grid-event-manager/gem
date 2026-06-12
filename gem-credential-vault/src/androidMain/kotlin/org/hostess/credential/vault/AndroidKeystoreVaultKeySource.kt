package org.hostess.credential.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidKeystoreVaultKeySource(
    private val alias: String = KEY_ALIAS,
) : VaultKeySource {
    override fun getOrCreateKey(): VaultKeySourceResult =
        try {
            val keyStore = loadedKeyStore()
            val key = keyStore.getKey(alias, null) as? SecretKey ?: generateKey()
            VaultKeySourceResult.Loaded(JcaVaultKeyMaterial(key, requiresProviderGeneratedIv = true))
        } catch (_: GeneralSecurityException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: IOException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: IllegalArgumentException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: IllegalStateException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        } catch (_: ProviderException) {
            VaultKeySourceResult.KeySourceFailed("key_source_failed")
        }

    override fun deleteKey(): VaultKeySourceDeleteResult =
        try {
            val keyStore = loadedKeyStore()
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                VaultKeySourceDeleteResult.Deleted
            } else {
                VaultKeySourceDeleteResult.Missing
            }
        } catch (_: GeneralSecurityException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        } catch (_: IOException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        } catch (_: IllegalArgumentException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        } catch (_: IllegalStateException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        } catch (_: ProviderException) {
            VaultKeySourceDeleteResult.KeySourceFailed("key_source_failed")
        }

    private fun loadedKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(AES_256_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    companion object {
        const val KEY_ALIAS: String = "org.hostess.vault.master.v1"

        private const val ANDROID_KEYSTORE: String = "AndroidKeyStore"
        private const val AES_256_BITS: Int = 256
    }
}
