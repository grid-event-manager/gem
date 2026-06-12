package org.hostess.credential.vault

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import org.hostess.core.ports.CredentialHandle

const val VAULT_MAGIC: String = "HSTSVLT1"
const val VAULT_PAYLOAD_VERSION: UShort = 1u
const val VAULT_FORMAT_VERSION: UShort = 1u
const val VAULT_MAX_STRING_BYTES: Int = 16 * 1024
const val VAULT_MAX_RECORD_COUNT: Int = 10_000

enum class HostessVaultCipherSuite(val code: UShort) {
    AES_256_GCM_NO_PADDING(1u),
}

data class HostessVaultFileHeader(
    val magic: String = VAULT_MAGIC,
    val formatVersion: UShort = VAULT_FORMAT_VERSION,
    val cipherSuite: HostessVaultCipherSuite = HostessVaultCipherSuite.AES_256_GCM_NO_PADDING,
    val nonce: ByteArray,
)

data class HostessVaultPlaintext(
    val payloadVersion: UShort = VAULT_PAYLOAD_VERSION,
    val profiles: List<HostessVaultProfileRecord>,
    val credentials: List<HostessVaultCredentialRecord>,
) {
    init {
        require(profiles.size <= VAULT_MAX_RECORD_COUNT) { "Too many profile records." }
        require(credentials.size <= VAULT_MAX_RECORD_COUNT) { "Too many credential records." }
    }
}

data class HostessVaultProfileRecord(
    val profileId: AccountProfileId,
    val loginName: SecondLifeLoginName,
    val label: String,
    val credentialHandle: CredentialHandle,
    val startLocation: String?,
) {
    init {
        require(label.isNotBlank()) { "HostessVaultProfileRecord label cannot be blank." }
    }
}

class HostessVaultCredentialRecord(
    val credentialHandle: CredentialHandle,
    val loginUri: SecondLifeLoginUri,
    val loginName: SecondLifeLoginName,
    val sharedSecret: SharedSecret,
    val startLocation: String?,
) {
    override fun toString(): String =
        "HostessVaultCredentialRecord(credentialHandle=$credentialHandle, loginUri=$loginUri, loginName=$loginName, secret=[redacted], startLocation=$startLocation)"
}
