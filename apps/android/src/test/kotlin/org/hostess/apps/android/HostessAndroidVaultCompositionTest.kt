package org.hostess.apps.android

import java.io.File
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.ports.AccountProfileIdSource
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.AccountProfileStoreDeleteResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AccountProfileStoreSaveResult
import org.hostess.core.ports.AccountProfileStoreUpdateResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultDeleteResult
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult
import org.hostess.core.ports.CredentialVaultUpdateResult
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeResetReason
import org.hostess.core.services.HostessCredentialRuntimeResetRequired
import org.hostess.core.services.HostessCredentialRuntimeUnavailable
import org.hostess.core.services.HostessCredentialRuntimeUnavailableReason
import org.hostess.credential.vault.VaultAccessOpenResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class HostessAndroidVaultCompositionTest {
    @Test
    fun `maps ready vault access result to runtime access carrier`() {
        val access = HostessAndroidVaultComposition.mapOpenResult(readyResult())

        assertIs<HostessCredentialRuntimeReady>(access.credentialRuntimeState)
        assertSame(FakeCredentialVault, access.credentialVault)
    }

    @Test
    fun `maps unavailable and reset-required vault failures exactly`() {
        assertUnavailable(
            VaultAccessOpenResult.KeySourceFailed("key_source_failed"),
            HostessCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
            "key_source_failed",
        )
        assertUnavailable(
            VaultAccessOpenResult.StorageFailed("storage_failed"),
            HostessCredentialRuntimeUnavailableReason.STORAGE_FAILED,
            "storage_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CryptoFailed("crypto_failed"),
            HostessCredentialRuntimeResetReason.CRYPTO_FAILED,
            "crypto_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CorruptVault("corrupt"),
            HostessCredentialRuntimeResetReason.CORRUPT_VAULT,
            "corrupt",
        )
    }

    @Test
    fun `uses app private Hostess vault file path`() {
        val appFilesDir = File("/tmp/hostess-test-app-files")

        assertEquals(
            File(appFilesDir, "Hostess/vault/vault.bin").path,
            HostessAndroidVaultComposition.vaultFile(appFilesDir).path,
        )
    }

    private fun assertUnavailable(
        openResult: VaultAccessOpenResult,
        reason: HostessCredentialRuntimeUnavailableReason,
        message: String,
    ) {
        val access = HostessAndroidVaultComposition.mapOpenResult(openResult)
        val state = assertIs<HostessCredentialRuntimeUnavailable>(access.credentialRuntimeState)
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
        assertNull(access.credentialVault)
    }

    private fun assertResetRequired(
        openResult: VaultAccessOpenResult,
        reason: HostessCredentialRuntimeResetReason,
        message: String,
    ) {
        val access = HostessAndroidVaultComposition.mapOpenResult(openResult)
        val state = assertIs<HostessCredentialRuntimeResetRequired>(access.credentialRuntimeState)
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
        CredentialVaultSaveResult.Saved(CredentialHandle("hostess-vault:v1:test"))

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
