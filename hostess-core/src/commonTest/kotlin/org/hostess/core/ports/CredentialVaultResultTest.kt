package org.hostess.core.ports

import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CredentialVaultResultTest {
    @Test
    fun `vault result taxonomy carries saved updated deleted resolved and failures`() {
        val handle = CredentialHandle("credential:v1:one")
        val material = LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName("jackraybold"),
            sharedSecret = assertNotNull(SharedSecret.fromPlainText("venue-password")),
            startLocation = null,
        )

        assertEquals(handle, CredentialVaultSaveResult.Saved(handle).credentialHandle)
        assertEquals(handle, CredentialVaultUpdateResult.Updated(handle).credentialHandle)
        assertEquals(handle, CredentialVaultDeleteResult.Deleted(handle).credentialHandle)
        assertEquals(material, CredentialVaultResolveResult.Resolved(material).material)

        val failures = listOf(
            CredentialVaultSaveResult.KeySourceFailed("[redacted]"),
            CredentialVaultSaveResult.CryptoFailed("[redacted]"),
            CredentialVaultSaveResult.CorruptVault("[redacted]"),
            CredentialVaultSaveResult.StorageFailed("[redacted]"),
            CredentialVaultUpdateResult.Missing(handle, "[redacted]"),
            CredentialVaultDeleteResult.Missing(handle, "[redacted]"),
            CredentialVaultResolveResult.Missing(handle, "[redacted]"),
        )

        failures.forEach { result ->
            assertFalse(result.toString().contains("venue-password"), result.toString())
        }
    }

    @Test
    fun `profile store result taxonomy carries list mutation and failure results`() {
        val profileId = org.hostess.core.domain.AccountProfileId("profile:v1:one")
        val profile = org.hostess.core.domain.SavedAccountProfile(
            profileId = profileId,
            loginName = loginName("jackraybold"),
            label = "jackraybold resident",
            credentialHandle = CredentialHandle("credential:v1:one"),
            startLocation = null,
        )

        assertEquals(listOf(profile), AccountProfileStoreListResult.Listed(listOf(profile)).profiles)
        assertEquals(profile, AccountProfileStoreSaveResult.Saved(profile).profile)
        assertEquals(profile, AccountProfileStoreUpdateResult.Updated(profile).profile)
        assertEquals(profileId, AccountProfileStoreDeleteResult.Deleted(profileId).profileId)

        val failures = listOf(
            AccountProfileStoreListResult.CorruptVault("[redacted]"),
            AccountProfileStoreSaveResult.StorageFailed("[redacted]"),
            AccountProfileStoreUpdateResult.Missing(profileId, "[redacted]"),
            AccountProfileStoreDeleteResult.Missing(profileId, "[redacted]"),
        )

        failures.forEach { result ->
            assertFalse(result.toString().contains("venue-password"), result.toString())
        }
    }

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName
}
