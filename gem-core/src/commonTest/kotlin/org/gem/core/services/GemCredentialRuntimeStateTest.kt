package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.ports.AccountProfileIdSource
import org.gem.core.ports.AccountProfileStore
import org.gem.core.ports.AccountProfileStoreDeleteResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.ports.AccountProfileStoreSaveResult
import org.gem.core.ports.AccountProfileStoreUpdateResult
import org.gem.core.ports.CredentialVault
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs

class GemCredentialRuntimeStateTest {
    @Test
    fun `runtime state exposes ready unavailable and reset-required shapes`() {
        val credentialService = CredentialService(
            accountProfileStore = EmptyAccountProfileStore,
            credentialVault = EmptyCredentialVault,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:one") },
        )

        val ready: GemCredentialRuntimeState = GemCredentialRuntimeReady(credentialService)
        val unavailable: GemCredentialRuntimeState = GemCredentialRuntimeUnavailable(
            reason = GemCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
            message = "[redacted]",
        )
        val resetRequired: GemCredentialRuntimeState = GemCredentialRuntimeResetRequired(
            reason = GemCredentialRuntimeResetReason.CORRUPT_VAULT,
            message = "[redacted]",
        )

        assertIs<GemCredentialRuntimeReady>(ready)
        assertIs<GemCredentialRuntimeUnavailable>(unavailable)
        assertIs<GemCredentialRuntimeResetRequired>(resetRequired)
        assertFalse(unavailable.toString().contains("venue-password"), unavailable.toString())
        assertFalse(resetRequired.toString().contains("venue-password"), resetRequired.toString())
    }
}

private object EmptyAccountProfileStore : AccountProfileStore {
    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(emptyList())

    override fun save(profile: org.gem.core.domain.SavedAccountProfile): AccountProfileStoreSaveResult =
        AccountProfileStoreSaveResult.Saved(profile)

    override fun update(profile: org.gem.core.domain.SavedAccountProfile): AccountProfileStoreUpdateResult =
        AccountProfileStoreUpdateResult.Updated(profile)

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        AccountProfileStoreDeleteResult.Deleted(profileId)
}

private object EmptyCredentialVault : CredentialVault {
    override fun save(material: org.gem.core.domain.LoginCredentialMaterial): CredentialVaultSaveResult =
        CredentialVaultSaveResult.StorageFailed("[redacted]")

    override fun update(
        credentialHandle: org.gem.core.ports.CredentialHandle,
        material: org.gem.core.domain.LoginCredentialMaterial,
    ): CredentialVaultUpdateResult =
        CredentialVaultUpdateResult.StorageFailed("[redacted]")

    override fun delete(credentialHandle: org.gem.core.ports.CredentialHandle): CredentialVaultDeleteResult =
        CredentialVaultDeleteResult.StorageFailed("[redacted]")

    override fun resolve(credentialHandle: org.gem.core.ports.CredentialHandle): CredentialVaultResolveResult =
        CredentialVaultResolveResult.StorageFailed("[redacted]")
}
