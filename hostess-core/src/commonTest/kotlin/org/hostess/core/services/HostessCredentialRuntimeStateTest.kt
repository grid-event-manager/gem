package org.hostess.core.services

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.ports.AccountProfileIdSource
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.AccountProfileStoreDeleteResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AccountProfileStoreSaveResult
import org.hostess.core.ports.AccountProfileStoreUpdateResult
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultDeleteResult
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult
import org.hostess.core.ports.CredentialVaultUpdateResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs

class HostessCredentialRuntimeStateTest {
    @Test
    fun `runtime state exposes ready unavailable and reset-required shapes`() {
        val credentialService = CredentialService(
            accountProfileStore = EmptyAccountProfileStore,
            credentialVault = EmptyCredentialVault,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:one") },
        )

        val ready: HostessCredentialRuntimeState = HostessCredentialRuntimeReady(credentialService)
        val unavailable: HostessCredentialRuntimeState = HostessCredentialRuntimeUnavailable(
            reason = HostessCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
            message = "[redacted]",
        )
        val resetRequired: HostessCredentialRuntimeState = HostessCredentialRuntimeResetRequired(
            reason = HostessCredentialRuntimeResetReason.CORRUPT_VAULT,
            message = "[redacted]",
        )

        assertIs<HostessCredentialRuntimeReady>(ready)
        assertIs<HostessCredentialRuntimeUnavailable>(unavailable)
        assertIs<HostessCredentialRuntimeResetRequired>(resetRequired)
        assertFalse(unavailable.toString().contains("venue-password"), unavailable.toString())
        assertFalse(resetRequired.toString().contains("venue-password"), resetRequired.toString())
    }
}

private object EmptyAccountProfileStore : AccountProfileStore {
    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(emptyList())

    override fun save(profile: org.hostess.core.domain.SavedAccountProfile): AccountProfileStoreSaveResult =
        AccountProfileStoreSaveResult.Saved(profile)

    override fun update(profile: org.hostess.core.domain.SavedAccountProfile): AccountProfileStoreUpdateResult =
        AccountProfileStoreUpdateResult.Updated(profile)

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        AccountProfileStoreDeleteResult.Deleted(profileId)
}

private object EmptyCredentialVault : CredentialVault {
    override fun save(material: org.hostess.core.domain.LoginCredentialMaterial): CredentialVaultSaveResult =
        CredentialVaultSaveResult.StorageFailed("[redacted]")

    override fun update(
        credentialHandle: org.hostess.core.ports.CredentialHandle,
        material: org.hostess.core.domain.LoginCredentialMaterial,
    ): CredentialVaultUpdateResult =
        CredentialVaultUpdateResult.StorageFailed("[redacted]")

    override fun delete(credentialHandle: org.hostess.core.ports.CredentialHandle): CredentialVaultDeleteResult =
        CredentialVaultDeleteResult.StorageFailed("[redacted]")

    override fun resolve(credentialHandle: org.hostess.core.ports.CredentialHandle): CredentialVaultResolveResult =
        CredentialVaultResolveResult.StorageFailed("[redacted]")
}
