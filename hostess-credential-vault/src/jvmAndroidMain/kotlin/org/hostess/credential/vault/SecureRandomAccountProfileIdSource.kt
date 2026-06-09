package org.hostess.credential.vault

import java.security.SecureRandom
import java.util.Base64
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.ports.AccountProfileIdSource

class SecureRandomAccountProfileIdSource(
    private val secureRandom: SecureRandom = SecureRandom(),
) : AccountProfileIdSource {
    override fun nextProfileId(): AccountProfileId =
        AccountProfileId("${AccountProfileId.PREFIX}${VaultRandomId.nextBase64UrlId(secureRandom)}")
}

internal object VaultRandomId {
    private const val ID_BYTES: Int = 16

    fun nextBase64UrlId(secureRandom: SecureRandom): String {
        val bytes = ByteArray(ID_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
