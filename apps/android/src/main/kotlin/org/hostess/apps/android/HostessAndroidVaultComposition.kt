package org.hostess.apps.android

import java.io.File
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.credential.vault.AndroidKeystoreVaultKeySource
import org.hostess.credential.vault.HostessVaultFileStore
import org.hostess.credential.vault.HostessVaultRuntimeAccess
import org.hostess.credential.vault.VaultAccessOpenResult
import org.hostess.credential.vault.VaultAccessService
import org.hostess.credential.vault.VaultCredentialRuntimeStateMapper

object HostessAndroidVaultComposition {
    fun create(appFilesDir: File): HostessCredentialRuntimeState =
        open(appFilesDir).credentialRuntimeState

    fun open(appFilesDir: File): HostessVaultRuntimeAccess =
        mapOpenResult(openVault(appFilesDir))

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): HostessVaultRuntimeAccess =
        HostessVaultRuntimeAccess(
            credentialRuntimeState = VaultCredentialRuntimeStateMapper.from(openResult),
            credentialVault = (openResult as? VaultAccessOpenResult.Ready)?.credentialVault,
        )

    internal fun vaultFile(appFilesDir: File): File =
        File(appFilesDir, "Hostess/vault/vault.bin")

    private fun openVault(appFilesDir: File): VaultAccessOpenResult =
        VaultAccessService(
            keySource = AndroidKeystoreVaultKeySource(),
            fileStore = HostessVaultFileStore(vaultFile(appFilesDir).toPath()),
        ).open()
}
