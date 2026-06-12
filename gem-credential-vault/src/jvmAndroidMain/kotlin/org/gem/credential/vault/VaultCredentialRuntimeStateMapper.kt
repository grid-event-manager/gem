package org.gem.credential.vault

import org.gem.core.services.CredentialService
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GemCredentialRuntimeResetReason
import org.gem.core.services.GemCredentialRuntimeResetRequired
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.core.services.GemCredentialRuntimeUnavailable
import org.gem.core.services.GemCredentialRuntimeUnavailableReason

object VaultCredentialRuntimeStateMapper {
    fun from(openResult: VaultAccessOpenResult): GemCredentialRuntimeState =
        when (openResult) {
            is VaultAccessOpenResult.Ready -> GemCredentialRuntimeReady(
                CredentialService(
                    accountProfileStore = openResult.accountProfileStore,
                    credentialVault = openResult.credentialVault,
                    accountProfileIdSource = openResult.accountProfileIdSource,
                ),
            )
            is VaultAccessOpenResult.KeySourceFailed -> GemCredentialRuntimeUnavailable(
                reason = GemCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.StorageFailed -> GemCredentialRuntimeUnavailable(
                reason = GemCredentialRuntimeUnavailableReason.STORAGE_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.CryptoFailed -> GemCredentialRuntimeResetRequired(
                reason = GemCredentialRuntimeResetReason.CRYPTO_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.CorruptVault -> GemCredentialRuntimeResetRequired(
                reason = GemCredentialRuntimeResetReason.CORRUPT_VAULT,
                message = openResult.message,
            )
        }
}
