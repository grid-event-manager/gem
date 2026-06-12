package org.gem.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.ports.AccountProfileIdSource
import org.gem.core.ports.AccountProfileStore
import org.gem.core.ports.AccountProfileStoreDeleteResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.ports.AccountProfileStoreSaveResult
import org.gem.core.ports.AccountProfileStoreUpdateResult
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.CredentialVault
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GemCredentialRuntimeResetReason
import org.gem.core.services.GemCredentialRuntimeResetRequired
import org.gem.core.services.GemCredentialRuntimeUnavailable
import org.gem.core.services.GemCredentialRuntimeUnavailableReason
import org.gem.credential.vault.LocalUserFileVaultKeySource
import org.gem.credential.vault.VaultAccessOpenResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopVaultCompositionTest {
    @Test
    fun `creates desktop credential runtime state from temp app data`() {
        val tempDataHome = Files.createTempDirectory("hostess-desktop-vault-test")
        try {
            val state = DesktopVaultComposition.create(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertIs<GemCredentialRuntimeReady>(state)
            assertTrue(Files.exists(tempDataHome.resolve("Hostess/vault/vault.bin")))
            assertTrue(Files.exists(tempDataHome.resolve("Hostess/vault/${LocalUserFileVaultKeySource.KEY_FILE_NAME}")))
        } finally {
            tempDataHome.deleteRecursively()
        }
    }

    @Test
    fun `maps unavailable and reset-required vault failures exactly`() {
        assertUnavailable(
            VaultAccessOpenResult.KeySourceFailed("key_source_failed"),
            GemCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
            "key_source_failed",
        )
        assertUnavailable(
            VaultAccessOpenResult.StorageFailed("storage_failed"),
            GemCredentialRuntimeUnavailableReason.STORAGE_FAILED,
            "storage_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CryptoFailed("crypto_failed"),
            GemCredentialRuntimeResetReason.CRYPTO_FAILED,
            "crypto_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CorruptVault("corrupt"),
            GemCredentialRuntimeResetReason.CORRUPT_VAULT,
            "corrupt",
        )
    }

    private fun assertUnavailable(
        openResult: VaultAccessOpenResult,
        reason: GemCredentialRuntimeUnavailableReason,
        message: String,
    ) {
        val access = DesktopVaultComposition.mapOpenResult(openResult)
        val state = assertIs<GemCredentialRuntimeUnavailable>(access.credentialRuntimeState)
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
        assertNull(access.credentialVault)
    }

    private fun assertResetRequired(
        openResult: VaultAccessOpenResult,
        reason: GemCredentialRuntimeResetReason,
        message: String,
    ) {
        val access = DesktopVaultComposition.mapOpenResult(openResult)
        val state = assertIs<GemCredentialRuntimeResetRequired>(access.credentialRuntimeState)
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
        assertNull(access.credentialVault)
    }

    @Test
    fun `maps ready vault access result to runtime access carrier`() {
        val ready = VaultAccessOpenResult.Ready(
            credentialVault = FakeDesktopCredentialVault,
            accountProfileStore = FakeDesktopAccountProfileStore,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:desktop") },
        )

        val access = DesktopVaultComposition.mapOpenResult(ready)

        assertIs<GemCredentialRuntimeReady>(access.credentialRuntimeState)
        assertSame(FakeDesktopCredentialVault, access.credentialVault)
    }

    private fun Path.deleteRecursively() {
        if (!Files.exists(this)) {
            return
        }
        Files.walk(this).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}

private object FakeDesktopAccountProfileStore : AccountProfileStore {
    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(emptyList())

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult =
        AccountProfileStoreSaveResult.Saved(profile)

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult =
        AccountProfileStoreUpdateResult.Updated(profile)

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        AccountProfileStoreDeleteResult.Deleted(profileId)
}

private object FakeDesktopCredentialVault : CredentialVault {
    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult =
        CredentialVaultSaveResult.Saved(CredentialHandle("gem-vault:v1:desktop"))

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult =
        CredentialVaultUpdateResult.Updated(credentialHandle)

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult =
        CredentialVaultDeleteResult.Deleted(credentialHandle)

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult =
        CredentialVaultResolveResult.Missing(credentialHandle)
}
