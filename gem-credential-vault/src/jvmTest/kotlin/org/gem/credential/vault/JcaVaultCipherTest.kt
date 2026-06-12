package org.gem.credential.vault

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JcaVaultCipherTest {
    @Test
    fun `encrypts and decrypts with aes gcm`() {
        val cipher = JcaVaultCipher()
        val plaintext = "synthetic vault payload".encodeToByteArray()

        val encrypted = assertIs<JcaVaultCipherEncryptResult.Encrypted>(cipher.encrypt(plaintext, key(1)))
        val decrypted = assertIs<JcaVaultCipherDecryptResult.Decrypted>(cipher.decrypt(encrypted.bytes, key(1)))

        assertContentEquals(plaintext, decrypted.plaintext)
        assertFalse(encrypted.bytes.containsSubsequence(plaintext))
        assertEquals(JcaVaultCipher.ALGORITHM, "AES/GCM/NoPadding")
    }

    @Test
    fun `uses fresh nonce for repeated encrypt calls`() {
        val cipher = JcaVaultCipher()
        val plaintext = "synthetic vault payload".encodeToByteArray()

        val first = assertIs<JcaVaultCipherEncryptResult.Encrypted>(cipher.encrypt(plaintext, key(1))).bytes
        val second = assertIs<JcaVaultCipherEncryptResult.Encrypted>(cipher.encrypt(plaintext, key(1))).bytes

        assertFalse(first.copyOfRange(12, 24).contentEquals(second.copyOfRange(12, 24)))
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `wrong key and tampered ciphertext fail authentication`() {
        val cipher = JcaVaultCipher()
        val encrypted = assertIs<JcaVaultCipherEncryptResult.Encrypted>(
            cipher.encrypt("synthetic vault payload".encodeToByteArray(), key(1)),
        ).bytes
        val tamperedCiphertext = encrypted.copyOf().also { it[it.lastIndex] = (it.last() xor 1) }

        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(encrypted, key(2)))
        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(tamperedCiphertext, key(1)))
    }

    @Test
    fun `tampered authenticated header fails as crypto failure`() {
        val cipher = JcaVaultCipher()
        val encrypted = assertIs<JcaVaultCipherEncryptResult.Encrypted>(
            cipher.encrypt("synthetic vault payload".encodeToByteArray(), key(1)),
        ).bytes
        val badMagic = encrypted.copyOf().also { it[0] = (it[0] xor 1) }
        val badVersion = encrypted.copyOf().also { it[9] = (it[9] xor 1) }
        val badSuite = encrypted.copyOf().also { it[11] = (it[11] xor 1) }
        val badNonce = encrypted.copyOf().also { it[12] = (it[12] xor 1) }

        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(badMagic, key(1)))
        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(badVersion, key(1)))
        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(badSuite, key(1)))
        assertIs<JcaVaultCipherDecryptResult.CryptoFailed>(cipher.decrypt(badNonce, key(1)))
    }

    @Test
    fun `corrupt envelope shape is corrupt vault`() {
        val cipher = JcaVaultCipher()
        val encrypted = assertIs<JcaVaultCipherEncryptResult.Encrypted>(
            cipher.encrypt("synthetic vault payload".encodeToByteArray(), key(1)),
        ).bytes
        val badLength = encrypted.copyOf().also {
            it[24] = 0
            it[25] = 0
            it[26] = 0
            it[27] = 1
        }

        assertIs<JcaVaultCipherDecryptResult.CorruptVault>(cipher.decrypt(byteArrayOf(1, 2, 3), key(1)))
        assertIs<JcaVaultCipherDecryptResult.CorruptVault>(cipher.decrypt(badLength, key(1)))
    }

    @Test
    fun `key material stays opaque and validates raw key size`() {
        assertNull(JcaVaultKeyMaterial.fromRawKeyBytes(ByteArray(31)))
        val material = assertNotNull(JcaVaultKeyMaterial.fromRawKeyBytes(ByteArray(32)))

        assertEquals("[redacted]", material.toString())
        assertIs<JcaVaultCipherEncryptResult.CryptoFailed>(
            JcaVaultCipher().encrypt("payload".encodeToByteArray(), UnsupportedVaultKeyMaterial),
        )
    }

    private fun key(seed: Int): JcaVaultKeyMaterial {
        val raw = ByteArray(32) { index -> (seed + index).toByte() }
        return assertNotNull(JcaVaultKeyMaterial.fromRawKeyBytes(raw))
    }
}

private object UnsupportedVaultKeyMaterial : VaultKeyMaterial

private infix fun Byte.xor(value: Int): Byte =
    (toInt() xor value).toByte()

private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean {
    for (index in 0..(size - needle.size)) {
        var found = true
        for (needleIndex in needle.indices) {
            if (this[index + needleIndex] != needle[needleIndex]) {
                found = false
                break
            }
        }
        if (found) {
            return true
        }
    }
    return false
}
