package org.gem.apps.android

import org.gem.credential.vault.AndroidKeystoreVaultKeySource
import org.gem.credential.vault.JcaVaultCipher
import org.gem.credential.vault.JcaVaultCipherDecryptResult
import org.gem.credential.vault.JcaVaultCipherEncryptResult
import org.gem.credential.vault.VaultKeySourceDeleteResult
import org.gem.credential.vault.VaultKeySourceResult
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
