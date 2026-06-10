package org.hostess.credential.vault

import org.hostess.core.services.CredentialService
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeResetReason
import org.hostess.core.services.HostessCredentialRuntimeResetRequired
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.core.services.HostessCredentialRuntimeUnavailable
import org.hostess.core.services.HostessCredentialRuntimeUnavailableReason

object VaultCredentialRuntimeStateMapper {
    fun from(openResult: VaultAccessOpenResult): HostessCredentialRuntimeState =
        when (openResult) {
            is VaultAccessOpenResult.Ready -> HostessCredentialRuntimeReady(
                CredentialService(
                    accountProfileStore = openResult.accountProfileStore,
                    credentialVault = openResult.credentialVault,
                    accountProfileIdSource = openResult.accountProfileIdSource,
                ),
            )
            is VaultAccessOpenResult.KeySourceFailed -> HostessCredentialRuntimeUnavailable(
                reason = HostessCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.StorageFailed -> HostessCredentialRuntimeUnavailable(
                reason = HostessCredentialRuntimeUnavailableReason.STORAGE_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.CryptoFailed -> HostessCredentialRuntimeResetRequired(
                reason = HostessCredentialRuntimeResetReason.CRYPTO_FAILED,
                message = openResult.message,
            )
            is VaultAccessOpenResult.CorruptVault -> HostessCredentialRuntimeResetRequired(
                reason = HostessCredentialRuntimeResetReason.CORRUPT_VAULT,
                message = openResult.message,
            )
        }
}
