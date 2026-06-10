package org.hostess.core.ports

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.HostessSession

interface SessionPort {
    fun login(request: LoginRequest): SessionLoginResult

    fun logout(session: HostessSession): SessionLogoutResult
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

    fun isHostessVaultCredentialHandle(): Boolean =
        value.startsWith(HOSTESS_VAULT_CREDENTIAL_PREFIX) &&
            value.removePrefix(HOSTESS_VAULT_CREDENTIAL_PREFIX).isNotBlank()

    companion object {
        const val HOSTESS_VAULT_CREDENTIAL_PREFIX: String = "hostess-vault:v1:"
    }
}

sealed interface SessionLoginResult {
    data class Success(val session: HostessSession) : SessionLoginResult
    data class Failure(val failure: CoreFailure) : SessionLoginResult
}

sealed interface SessionLogoutResult {
    data object LoggedOut : SessionLogoutResult
    data class Failure(val failure: CoreFailure) : SessionLogoutResult
}
