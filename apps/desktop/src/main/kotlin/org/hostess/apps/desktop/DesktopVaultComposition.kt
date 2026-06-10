package org.hostess.apps.desktop

import java.nio.file.Path
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.credential.vault.DesktopVaultPaths
import org.hostess.credential.vault.HostessVaultFileStore
import org.hostess.credential.vault.HostessVaultRuntimeAccess
import org.hostess.credential.vault.LocalUserFileVaultKeySource
import org.hostess.credential.vault.VaultAccessOpenResult
import org.hostess.credential.vault.VaultAccessService
import org.hostess.credential.vault.VaultCredentialRuntimeStateMapper

object DesktopVaultComposition {
    fun create(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): HostessCredentialRuntimeState =
        open(osName, env, userHome).credentialRuntimeState

    fun open(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): HostessVaultRuntimeAccess {
        val vaultDirectory = Path.of(DesktopVaultPaths.defaultVaultDirectory(osName, env, userHome))
        return mapOpenResult(
            VaultAccessService(
                keySource = LocalUserFileVaultKeySource(vaultDirectory, osName),
                fileStore = HostessVaultFileStore(vaultDirectory.resolve(VAULT_FILE_NAME)),
            ).open(),
        )
    }

    internal fun mapOpenResult(openResult: VaultAccessOpenResult): HostessVaultRuntimeAccess =
        HostessVaultRuntimeAccess(
            credentialRuntimeState = VaultCredentialRuntimeStateMapper.from(openResult),
            credentialVault = (openResult as? VaultAccessOpenResult.Ready)?.credentialVault,
        )

    private const val VAULT_FILE_NAME: String = "vault.bin"
}
