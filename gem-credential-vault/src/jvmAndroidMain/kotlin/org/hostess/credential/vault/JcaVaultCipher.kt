package org.hostess.credential.vault

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class JcaVaultCipher(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun encrypt(
        plaintext: ByteArray,
        key: VaultKeyMaterial,
    ): JcaVaultCipherEncryptResult {
        val jcaKey = key as? JcaVaultKeyMaterial
            ?: return JcaVaultCipherEncryptResult.CryptoFailed("unsupported_key_material")
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val nonce = if (jcaKey.requiresProviderGeneratedIv) {
                cipher.init(Cipher.ENCRYPT_MODE, jcaKey.secretKey)
                cipher.iv?.takeIf { it.size == NONCE_BYTES }
                    ?: return JcaVaultCipherEncryptResult.CryptoFailed("crypto_failed")
            } else {
                ByteArray(NONCE_BYTES).also { generatedNonce ->
                    secureRandom.nextBytes(generatedNonce)
                    cipher.init(Cipher.ENCRYPT_MODE, jcaKey.secretKey, GCMParameterSpec(TAG_BITS, generatedNonce))
                }
            }
            val header = VaultEnvelope.header(nonce)
            cipher.updateAAD(header)
            val ciphertext = cipher.doFinal(plaintext)
            JcaVaultCipherEncryptResult.Encrypted(VaultEnvelope.encode(header, ciphertext))
        } catch (_: GeneralSecurityException) {
            JcaVaultCipherEncryptResult.CryptoFailed("crypto_failed")
        } catch (_: IllegalArgumentException) {
            JcaVaultCipherEncryptResult.CorruptVault("corrupt")
        }
    }

    fun decrypt(
        envelopeBytes: ByteArray,
        key: VaultKeyMaterial,
    ): JcaVaultCipherDecryptResult {
        val jcaKey = key as? JcaVaultKeyMaterial
            ?: return JcaVaultCipherDecryptResult.CryptoFailed("unsupported_key_material")
        val envelope = VaultEnvelope.decode(envelopeBytes)
            ?: return JcaVaultCipherDecryptResult.CorruptVault("corrupt")
        if (!envelope.hasSupportedAuthenticatedHeader()) {
            return JcaVaultCipherDecryptResult.CryptoFailed("crypto_failed")
        }

        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, jcaKey.secretKey, GCMParameterSpec(TAG_BITS, envelope.nonce))
            cipher.updateAAD(envelope.header)
            JcaVaultCipherDecryptResult.Decrypted(cipher.doFinal(envelope.ciphertext))
        } catch (_: GeneralSecurityException) {
            JcaVaultCipherDecryptResult.CryptoFailed("crypto_failed")
        }
    }

    companion object {
        const val ALGORITHM: String = "AES/GCM/NoPadding"
        const val NONCE_BYTES: Int = 12
        const val TAG_BITS: Int = 128
    }
}

sealed interface JcaVaultCipherEncryptResult {
    data class Encrypted(val bytes: ByteArray) : JcaVaultCipherEncryptResult
    data class CryptoFailed(val message: String? = null) : JcaVaultCipherEncryptResult
    data class CorruptVault(val message: String? = null) : JcaVaultCipherEncryptResult
}

sealed interface JcaVaultCipherDecryptResult {
    data class Decrypted(val plaintext: ByteArray) : JcaVaultCipherDecryptResult
    data class CryptoFailed(val message: String? = null) : JcaVaultCipherDecryptResult
    data class CorruptVault(val message: String? = null) : JcaVaultCipherDecryptResult
}

private data class VaultEnvelope(
    val header: ByteArray,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
) {
    fun hasSupportedAuthenticatedHeader(): Boolean {
        val magic = header.copyOfRange(0, MAGIC_BYTES.size).decodeToString()
        val formatVersion = header.readUShort(MAGIC_BYTES.size)
        val cipherSuite = header.readUShort(MAGIC_BYTES.size + 2)
        return magic == VAULT_MAGIC &&
            formatVersion == VAULT_FORMAT_VERSION.toInt() &&
            cipherSuite == HostessVaultCipherSuite.AES_256_GCM_NO_PADDING.code.toInt()
    }

    companion object {
        private const val HEADER_BYTES: Int = 24
        private const val CIPHERTEXT_LENGTH_BYTES: Int = 4

        fun header(nonce: ByteArray): ByteArray {
            require(nonce.size == JcaVaultCipher.NONCE_BYTES) { "Vault nonce must be 12 bytes." }
            return buildList<Byte> {
                addAll(MAGIC_BYTES.toList())
                writeUShort(VAULT_FORMAT_VERSION.toInt())
                writeUShort(HostessVaultCipherSuite.AES_256_GCM_NO_PADDING.code.toInt())
                addAll(nonce.toList())
            }.toByteArray()
        }

        fun encode(header: ByteArray, ciphertext: ByteArray): ByteArray =
            buildList<Byte> {
                addAll(header.toList())
                writeUInt(ciphertext.size)
                addAll(ciphertext.toList())
            }.toByteArray()

        fun decode(bytes: ByteArray): VaultEnvelope? {
            if (bytes.size < HEADER_BYTES + CIPHERTEXT_LENGTH_BYTES) {
                return null
            }
            val header = bytes.copyOfRange(0, HEADER_BYTES)
            val ciphertextLength = bytes.readUInt(HEADER_BYTES) ?: return null
            if (ciphertextLength > Int.MAX_VALUE.toLong()) {
                return null
            }
            val ciphertextStart = HEADER_BYTES + CIPHERTEXT_LENGTH_BYTES
            val ciphertextEnd = ciphertextStart + ciphertextLength.toInt()
            if (ciphertextEnd != bytes.size) {
                return null
            }
            val nonce = header.copyOfRange(HEADER_BYTES - JcaVaultCipher.NONCE_BYTES, HEADER_BYTES)
            return VaultEnvelope(
                header = header,
                nonce = nonce,
                ciphertext = bytes.copyOfRange(ciphertextStart, ciphertextEnd),
            )
        }
    }
}

private val MAGIC_BYTES: ByteArray = VAULT_MAGIC.encodeToByteArray()

private fun MutableList<Byte>.writeUShort(value: Int) {
    add(((value ushr 8) and 0xFF).toByte())
    add((value and 0xFF).toByte())
}

private fun MutableList<Byte>.writeUInt(value: Int) {
    add(((value ushr 24) and 0xFF).toByte())
    add(((value ushr 16) and 0xFF).toByte())
    add(((value ushr 8) and 0xFF).toByte())
    add((value and 0xFF).toByte())
}

private fun ByteArray.readUShort(offset: Int): Int =
    (this[offset].unsigned() shl 8) or this[offset + 1].unsigned()

private fun ByteArray.readUInt(offset: Int): Long? {
    if (size - offset < 4) {
        return null
    }
    return (this[offset].unsigned().toLong() shl 24) or
        (this[offset + 1].unsigned().toLong() shl 16) or
        (this[offset + 2].unsigned().toLong() shl 8) or
        this[offset + 3].unsigned().toLong()
}

private fun Byte.unsigned(): Int = toInt() and 0xFF
