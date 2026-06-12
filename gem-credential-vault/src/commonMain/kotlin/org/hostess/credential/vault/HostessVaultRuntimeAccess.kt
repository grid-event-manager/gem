package org.hostess.credential.vault

import org.hostess.core.ports.CredentialVault
import org.hostess.core.services.HostessCredentialRuntimeState

data class HostessVaultRuntimeAccess(
    val credentialRuntimeState: HostessCredentialRuntimeState,
    val credentialVault: CredentialVault?,
)
