package org.gem.apps.android

import java.io.File
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.credential.vault.AndroidKeystoreVaultKeySource
import org.gem.credential.vault.GemVaultFileStore
import org.gem.credential.vault.GemVaultRuntimeAccess
import org.gem.credential.vault.VaultAccessOpenResult
import org.gem.credential.vault.VaultAccessService
import org.gem.credential.vault.VaultCredentialRuntimeStateMapper

object GemAndroidVaultComposition {
    fun create(appFilesDir: File): GemCredentialRuntimeState =
        open(appFilesDir).credentialRuntimeState

    fun open(appFilesDir: File): GemVaultRuntimeAccess =
        mapOpenResult(openVault(appFilesDir))

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): GemVaultRuntimeAccess =
        GemVaultRuntimeAccess(
            credentialRuntimeState = VaultCredentialRuntimeStateMapper.from(openResult),
            credentialVault = (openResult as? VaultAccessOpenResult.Ready)?.credentialVault,
        )

    internal fun vaultFile(appFilesDir: File): File =
        File(appFilesDir, "gem/vault/vault.bin")

    private fun openVault(appFilesDir: File): VaultAccessOpenResult =
        VaultAccessService(
            keySource = AndroidKeystoreVaultKeySource(),
            fileStore = GemVaultFileStore(vaultFile(appFilesDir).toPath()),
        ).open()
}
