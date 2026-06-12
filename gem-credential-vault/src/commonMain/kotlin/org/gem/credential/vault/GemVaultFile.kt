package org.gem.credential.vault

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SharedSecret
import org.gem.core.ports.CredentialHandle

const val VAULT_MAGIC: String = "HSTSVLT1"
const val VAULT_PAYLOAD_VERSION: UShort = 1u
const val VAULT_FORMAT_VERSION: UShort = 1u
const val VAULT_MAX_STRING_BYTES: Int = 16 * 1024
const val VAULT_MAX_RECORD_COUNT: Int = 10_000

enum class GemVaultCipherSuite(val code: UShort) {
    AES_256_GCM_NO_PADDING(1u),
}

data class GemVaultFileHeader(
    val magic: String = VAULT_MAGIC,
    val formatVersion: UShort = VAULT_FORMAT_VERSION,
    val cipherSuite: GemVaultCipherSuite = GemVaultCipherSuite.AES_256_GCM_NO_PADDING,
    val nonce: ByteArray,
)

data class GemVaultPlaintext(
    val payloadVersion: UShort = VAULT_PAYLOAD_VERSION,
    val profiles: List<GemVaultProfileRecord>,
    val credentials: List<GemVaultCredentialRecord>,
) {
    init {
        require(profiles.size <= VAULT_MAX_RECORD_COUNT) { "Too many profile records." }
        require(credentials.size <= VAULT_MAX_RECORD_COUNT) { "Too many credential records." }
    }
}

data class GemVaultProfileRecord(
    val profileId: AccountProfileId,
    val loginName: SecondLifeLoginName,
    val label: String,
    val credentialHandle: CredentialHandle,
    val startLocation: String?,
) {
    init {
        require(label.isNotBlank()) { "GemVaultProfileRecord label cannot be blank." }
    }
}

class GemVaultCredentialRecord(
    val credentialHandle: CredentialHandle,
    val loginUri: SecondLifeLoginUri,
    val loginName: SecondLifeLoginName,
    val sharedSecret: SharedSecret,
    val startLocation: String?,
) {
    override fun toString(): String =
        "GemVaultCredentialRecord(credentialHandle=$credentialHandle, loginUri=$loginUri, loginName=$loginName, secret=[redacted], startLocation=$startLocation)"
}
