package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultResolveResult

class CredentialVaultLoginSecretResolver(
    private val credentialVault: CredentialVault,
) : LoginSecretResolver {
    override fun resolve(handle: CredentialHandle): LoginSecret? {
        if (!handle.isHostessVaultCredentialHandle()) {
            return null
        }

        return when (val result = credentialVault.resolve(handle)) {
            is CredentialVaultResolveResult.Resolved -> result.material.toLoginSecret()
            is CredentialVaultResolveResult.Missing,
            is CredentialVaultResolveResult.KeySourceFailed,
            is CredentialVaultResolveResult.CryptoFailed,
            is CredentialVaultResolveResult.CorruptVault,
            is CredentialVaultResolveResult.StorageFailed,
            -> null
        }
    }

    private fun LoginCredentialMaterial.toLoginSecret(): LoginSecret =
        LoginSecret(
            loginUri = loginUri.value,
            firstName = loginName.firstName,
            lastName = loginName.lastName,
            sharedSecret = sharedSecret.revealForLogin(),
            startLocation = startLocation ?: DEFAULT_START_LOCATION,
        )

    private companion object {
        const val DEFAULT_START_LOCATION = "last"
    }
}
