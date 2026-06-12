package org.gem.credential.vault

import org.gem.core.ports.CredentialVault
import org.gem.core.services.GemCredentialRuntimeState

data class GemVaultRuntimeAccess(
    val credentialRuntimeState: GemCredentialRuntimeState,
    val credentialVault: CredentialVault?,
)
