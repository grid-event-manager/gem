package org.hostess.apps.android

import org.hostess.credential.vault.AndroidKeystoreVaultKeySource
import org.hostess.credential.vault.JcaVaultCipher
import org.hostess.credential.vault.JcaVaultCipherDecryptResult
import org.hostess.credential.vault.JcaVaultCipherEncryptResult
import org.hostess.credential.vault.VaultKeySourceDeleteResult
import org.hostess.credential.vault.VaultKeySourceResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeystoreVaultKeySourceInstrumentedTest {
    @Test
    fun createsLoadsUsesAndDeletesAndroidKeystoreAlias() {
        val source = AndroidKeystoreVaultKeySource()
        source.deleteKey()

        try {
            val first = source.getOrCreateKey() as VaultKeySourceResult.Loaded
            assertEquals("[redacted]", first.keyMaterial.toString())

            val cipher = JcaVaultCipher()
            val plaintext = "synthetic android keystore vault payload".encodeToByteArray()
            val encrypted = cipher.encrypt(plaintext, first.keyMaterial) as JcaVaultCipherEncryptResult.Encrypted

            val second = source.getOrCreateKey() as VaultKeySourceResult.Loaded
            val decrypted = cipher.decrypt(encrypted.bytes, second.keyMaterial) as JcaVaultCipherDecryptResult.Decrypted

            assertArrayEquals(plaintext, decrypted.plaintext)
            assertTrue(encrypted.bytes.isNotEmpty())
            assertEquals(VaultKeySourceDeleteResult.Deleted, source.deleteKey())
            assertEquals(VaultKeySourceDeleteResult.Missing, source.deleteKey())
        } finally {
            source.deleteKey()
        }
    }
}
