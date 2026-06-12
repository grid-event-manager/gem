package org.gem.credential.vault

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class JcaVaultKeyMaterial internal constructor(
    internal val secretKey: SecretKey,
    internal val requiresProviderGeneratedIv: Boolean = false,
) : VaultKeyMaterial {
    override fun toString(): String = "[redacted]"

    companion object {
        const val RAW_AES_256_KEY_BYTES: Int = 32

        fun fromRawKeyBytes(rawKey: ByteArray): JcaVaultKeyMaterial? {
            if (rawKey.size != RAW_AES_256_KEY_BYTES) {
                return null
            }
            return JcaVaultKeyMaterial(SecretKeySpec(rawKey.copyOf(), "AES"))
        }
    }
}
