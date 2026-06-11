package org.hostess.credential.vault

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult

class VaultAccessServiceTest {
    @Test
    fun `open initializes missing vault and lock closes ready services`() = withTempDirectory { dir ->
        val service = accessService(dir)
        val ready = assertIs<VaultAccessOpenResult.Ready>(service.open())
        val saved = assertIs<CredentialVaultSaveResult.Saved>(ready.credentialVault.save(material("venue-password")))
        assertTrue(Files.exists(dir.resolve("vault.bin")))

        service.lock()

        val locked = assertIs<CredentialVaultResolveResult.KeySourceFailed>(
            ready.credentialVault.resolve(saved.credentialHandle),
        )
        assertEquals("vault_locked", locked.message)
    }

    @Test
    fun `reset requires confirmation and then deletes vault plus key`() = withTempDirectory { dir ->
        val service = accessService(dir)
        assertIs<VaultAccessOpenResult.Ready>(service.open())
        assertTrue(Files.exists(dir.resolve("vault.bin")))
        assertTrue(Files.exists(dir.resolve("keys").resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)))

        val unconfirmed = assertIs<VaultAccessResetResult.StorageFailed>(service.reset(confirmed = false))
        assertEquals("reset_not_confirmed", unconfirmed.message)
        assertTrue(Files.exists(dir.resolve("vault.bin")))

        assertEquals(VaultAccessResetResult.ResetComplete, service.reset(confirmed = true))
        assertTrue(!Files.exists(dir.resolve("vault.bin")))
        assertTrue(!Files.exists(dir.resolve("keys").resolve(LocalUserFileVaultKeySource.KEY_FILE_NAME)))
    }

    @Test
    fun `lost desktop key with existing vault opens as crypto failure`() = withTempDirectory { dir ->
        val first = accessService(dir, keyByte = 9)
        val ready = assertIs<VaultAccessOpenResult.Ready>(first.open())
        assertIs<CredentialVaultSaveResult.Saved>(ready.credentialVault.save(material("venue-password")))

        LocalUserFileVaultKeySource(dir.resolve("keys")).deleteKey()

        val second = accessService(dir, keyByte = 10)
        assertIs<VaultAccessOpenResult.CryptoFailed>(second.open())
    }

    private fun accessService(
        dir: Path,
        keyByte: Int = 9,
    ): VaultAccessService =
        VaultAccessService(
            keySource = LocalUserFileVaultKeySource(dir.resolve("keys"), secureRandom = FillingSecureRandom(keyByte)),
            fileStore = HostessVaultFileStore(dir.resolve("vault.bin")),
            credentialHandleRandom = FillingSecureRandom(4),
        )

    private fun material(password: String): LoginCredentialMaterial =
        LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput("venuehost")).loginName,
            sharedSecret = requireNotNull(SharedSecret.fromPlainText(password)),
            startLocation = "uri:London City&76&174&23",
        )

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("hostess-vault-access-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
