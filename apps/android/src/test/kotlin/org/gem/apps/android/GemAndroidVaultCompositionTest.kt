package org.gem.apps.android

import java.io.File
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
import org.gem.credential.vault.VaultAccessOpenResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class GemAndroidVaultCompositionTest {
    @Test
    fun `maps ready vault access result to runtime access carrier`() {
        val access = GemAndroidVaultComposition.mapOpenResult(readyResult())

        assertIs<GemCredentialRuntimeReady>(access.credentialRuntimeState)
        assertSame(FakeCredentialVault, access.credentialVault)
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

    @Test
    fun `uses app private Hostess vault file path`() {
        val appFilesDir = File("/tmp/hostess-test-app-files")

        assertEquals(
            File(appFilesDir, "Hostess/vault/vault.bin").path,
            GemAndroidVaultComposition.vaultFile(appFilesDir).path,
        )
    }

    private fun assertUnavailable(
        openResult: VaultAccessOpenResult,
        reason: GemCredentialRuntimeUnavailableReason,
        message: String,
    ) {
        val access = GemAndroidVaultComposition.mapOpenResult(openResult)
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
        val access = GemAndroidVaultComposition.mapOpenResult(openResult)
        val state = assertIs<GemCredentialRuntimeResetRequired>(access.credentialRuntimeState)
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
        assertNull(access.credentialVault)
    }

    private fun readyResult(): VaultAccessOpenResult.Ready =
        VaultAccessOpenResult.Ready(
            credentialVault = FakeCredentialVault,
            accountProfileStore = FakeAccountProfileStore,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:test") },
        )
}

private object FakeAccountProfileStore : AccountProfileStore {
    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(emptyList())

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult =
        AccountProfileStoreSaveResult.Saved(profile)

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult =
        AccountProfileStoreUpdateResult.Updated(profile)

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        AccountProfileStoreDeleteResult.Deleted(profileId)
}

private object FakeCredentialVault : CredentialVault {
    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult =
        CredentialVaultSaveResult.Saved(CredentialHandle("gem-vault:v1:test"))

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
