package org.gem.protocol.libomv.runtime

import org.gem.core.domain.SecondLifeLoginSecretPolicy

internal class SecondLifePasswordHash private constructor(
    val wireValue: String,
) {
    companion object {
        fun fromSharedSecret(
            value: String,
            digestPort: Md5DigestPort,
        ): SecondLifePasswordHash? {
            val normalized = SecondLifeLoginSecretPolicy.normalizeForStorage(value) ?: return null
            if (normalized.startsWith("\$1\$")) {
                return SecondLifePasswordHash(normalized)
            }
            return SecondLifePasswordHash("\$1\$${digestPort.md5Hex(normalized.encodeToByteArray())}")
        }
    }
}
