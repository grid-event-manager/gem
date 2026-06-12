package org.gem.apps.desktop

import java.nio.file.Path
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.credential.vault.DesktopVaultPaths
import org.gem.credential.vault.GemVaultFileStore
import org.gem.credential.vault.GemVaultRuntimeAccess
import org.gem.credential.vault.LocalUserFileVaultKeySource
import org.gem.credential.vault.VaultAccessOpenResult
import org.gem.credential.vault.VaultAccessService
import org.gem.credential.vault.VaultCredentialRuntimeStateMapper

object DesktopVaultComposition {
    fun create(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): GemCredentialRuntimeState =
        open(osName, env, userHome).credentialRuntimeState

    fun open(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): GemVaultRuntimeAccess {
        val vaultDirectory = Path.of(DesktopVaultPaths.defaultVaultDirectory(osName, env, userHome))
        return mapOpenResult(
            VaultAccessService(
                keySource = LocalUserFileVaultKeySource(vaultDirectory, osName),
                fileStore = GemVaultFileStore(vaultDirectory.resolve(VAULT_FILE_NAME)),
            ).open(),
        )
    }

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): GemVaultRuntimeAccess =
        GemVaultRuntimeAccess(
            credentialRuntimeState = VaultCredentialRuntimeStateMapper.from(openResult),
            credentialVault = (openResult as? VaultAccessOpenResult.Ready)?.credentialVault,
        )

    private const val VAULT_FILE_NAME: String = "vault.bin"
}
