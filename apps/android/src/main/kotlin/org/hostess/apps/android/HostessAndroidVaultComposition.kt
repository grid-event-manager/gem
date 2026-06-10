package org.hostess.apps.android

import java.io.File
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.credential.vault.AndroidKeystoreVaultKeySource
import org.hostess.credential.vault.HostessVaultFileStore
import org.hostess.credential.vault.VaultAccessOpenResult
import org.hostess.credential.vault.VaultAccessService
import org.hostess.credential.vault.VaultCredentialRuntimeStateMapper

object HostessAndroidVaultComposition {
    fun create(appFilesDir: File): HostessCredentialRuntimeState =
        mapOpenResult(
            VaultAccessService(
                keySource = AndroidKeystoreVaultKeySource(),
                fileStore = HostessVaultFileStore(vaultFile(appFilesDir).toPath()),
            ).open(),
        )

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): HostessCredentialRuntimeState =
        VaultCredentialRuntimeStateMapper.from(openResult)

    internal fun vaultFile(appFilesDir: File): File =
        File(appFilesDir, "Hostess/vault/vault.bin")
}
