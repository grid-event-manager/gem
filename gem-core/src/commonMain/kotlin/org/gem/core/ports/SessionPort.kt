package org.gem.core.ports

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.GemSession

interface SessionPort {
    fun login(request: LoginRequest): SessionLoginResult

    fun logout(session: GemSession): SessionLogoutResult
}

data class LoginRequest(
    val accountLabel: AccountLabel,
    val credentialHandle: CredentialHandle,
)

@JvmInline
value class CredentialHandle(val value: String) {
    init {
        require(value.isNotBlank()) { "CredentialHandle cannot be blank." }
    }

    fun isGemVaultCredentialHandle(): Boolean =
        value.startsWith(HOSTESS_VAULT_CREDENTIAL_PREFIX) &&
            value.removePrefix(HOSTESS_VAULT_CREDENTIAL_PREFIX).isNotBlank()

    companion object {
        const val HOSTESS_VAULT_CREDENTIAL_PREFIX: String = "gem-vault:v1:"
    }
}

sealed interface SessionLoginResult {
    data class Success(val session: GemSession) : SessionLoginResult
    data class Failure(val failure: CoreFailure) : SessionLoginResult
}

sealed interface SessionLogoutResult {
    data object LoggedOut : SessionLogoutResult
    data class Failure(val failure: CoreFailure) : SessionLogoutResult
}
