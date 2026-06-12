package org.gem.core.domain

import org.gem.core.ports.CredentialHandle

@JvmInline
value class AccountProfileId(val value: String) {
    init {
        require(value.startsWith(PREFIX)) { "AccountProfileId must start with $PREFIX." }
        require(value.removePrefix(PREFIX).isNotBlank()) { "AccountProfileId suffix cannot be blank." }
    }

    companion object {
        const val PREFIX: String = "profile:v1:"
    }
}

data class SavedAccountProfile(
    val profileId: AccountProfileId,
    val loginName: SecondLifeLoginName,
    val label: String,
    val credentialHandle: CredentialHandle,
    val startLocation: String?,
) {
    init {
        require(label.isNotBlank()) { "SavedAccountProfile label cannot be blank." }
    }
}
