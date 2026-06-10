package org.hostess.apps.desktop

import java.nio.file.Path
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.credential.vault.DesktopVaultPaths
import org.hostess.credential.vault.HostessVaultFileStore
import org.hostess.credential.vault.LocalUserFileVaultKeySource
import org.hostess.credential.vault.VaultAccessOpenResult
import org.hostess.credential.vault.VaultAccessService
import org.hostess.credential.vault.VaultCredentialRuntimeStateMapper

object DesktopVaultComposition {
    fun create(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): HostessCredentialRuntimeState {
        val vaultDirectory = Path.of(DesktopVaultPaths.defaultVaultDirectory(osName, env, userHome))
        return mapOpenResult(
            VaultAccessService(
                keySource = LocalUserFileVaultKeySource(vaultDirectory, osName),
                fileStore = HostessVaultFileStore(vaultDirectory.resolve(VAULT_FILE_NAME)),
            ).open(),
        )
    }

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): HostessCredentialRuntimeState =
        VaultCredentialRuntimeStateMapper.from(openResult)

    private const val VAULT_FILE_NAME: String = "vault.bin"
}
