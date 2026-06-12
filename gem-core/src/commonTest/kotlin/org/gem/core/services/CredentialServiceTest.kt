package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameInvalidReason
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SharedSecret
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CredentialServiceTest {
    @Test
    fun `add login normalizes account stores credential then saves profile`() {
        val store = FakeAccountProfileStore()
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        val result = assertIs<CredentialServiceAddResult.Saved>(
            service.addLogin("VenueHost", "venue-password", startLocation = "uri:London City&76&174&23"),
        )

        assertEquals("venuehost resident", result.profile.loginName.value)
        assertEquals("venuehost resident", result.profile.label)
        assertEquals(CredentialHandle("credential:v1:1"), result.profile.credentialHandle)
        assertEquals(1, vault.savedMaterials.size)
        assertEquals("venue-password", vault.savedMaterials.single().sharedSecret.revealForLogin())
        assertEquals(listOf(result.profile), store.savedProfiles)
    }

    @Test
    fun `add login rejects invalid login name and blank secret before storage`() {
        val store = FakeAccountProfileStore()
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        val invalidName = assertIs<CredentialServiceAddResult.InvalidLoginName>(service.addLogin(" ", "password"))
        val invalidSecret = service.addLogin("venuehost", " ")

        assertEquals(SecondLifeLoginNameInvalidReason.BLANK, invalidName.reason)
        assertEquals(CredentialServiceAddResult.InvalidSecret, invalidSecret)
        assertTrue(vault.savedMaterials.isEmpty())
        assertTrue(store.savedProfiles.isEmpty())
    }

    @Test
    fun `add login attempts credential cleanup when profile save fails`() {
        val store = FakeAccountProfileStore(
            saveResult = AccountProfileStoreSaveResult.StorageFailed("[redacted]"),
        )
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        assertIs<CredentialServiceAddResult.ProfileStoreFailure>(
            service.addLogin("venuehost", "venue-password"),
        )

        assertEquals(listOf(CredentialHandle("credential:v1:1")), vault.deletedHandles)
    }

    @Test
    fun `list profiles delegates to profile store without resolving secrets`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        val result = assertIs<AccountProfileStoreListResult.Listed>(service.listProfiles())

        assertEquals(listOf(profile), result.profiles)
        assertEquals(1, store.listCalls)
        assertEquals(0, vault.resolveCalls)
    }

    @Test
    fun `update password preserves existing credential material fields`() {
        val profile = profile("one")
        val existing = material(
            password = "old-password",
            loginUri = SecondLifeLoginUri("https://login.secondlife.test/cgi-bin/login.cgi"),
            startLocation = "uri:London City&76&174&23",
        )
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault(resolveResult = CredentialVaultResolveResult.Resolved(existing))
        val service = service(store, vault)

        val result = service.updatePassword(profile.profileId, "new-password")

        assertEquals(CredentialServiceUpdatePasswordResult.Updated, result)
        val update = vault.updatedMaterials.single()
        assertEquals(profile.credentialHandle, update.first)
        assertEquals(existing.loginUri, update.second.loginUri)
        assertEquals(existing.loginName, update.second.loginName)
        assertEquals(existing.startLocation, update.second.startLocation)
        assertEquals("new-password", update.second.sharedSecret.revealForLogin())
    }

    @Test
    fun `update password reports missing profile and invalid secret`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        assertIs<CredentialServiceUpdatePasswordResult.MissingProfile>(
            service.updatePassword(AccountProfileId("profile:v1:missing"), "new-password"),
        )
        assertEquals(CredentialServiceUpdatePasswordResult.InvalidSecret, service.updatePassword(profile.profileId, " "))
        assertTrue(vault.updatedMaterials.isEmpty())
    }

    @Test
    fun `reveal password returns selected profile and redacts result string`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault(resolveResult = CredentialVaultResolveResult.Resolved(material("saved-password")))
        val service = service(store, vault)

        val result = assertIs<CredentialServiceRevealPasswordResult.Revealed>(
            service.revealPassword(profile.profileId),
        )

        assertEquals(profile, result.profile)
        assertEquals("saved-password", result.password)
        assertEquals(1, vault.resolveCalls)
        assertFalse(result.toString().contains("saved-password"))
        assertTrue(result.toString().contains("password=[redacted]"))
    }

    @Test
    fun `reveal password reports missing profile and store failure before vault resolve`() {
        val profile = profile("one")
        val missingStore = FakeAccountProfileStore(
            listResult = AccountProfileStoreListResult.Listed(listOf(profile)),
        )
        val failedStore = FakeAccountProfileStore(
            listResult = AccountProfileStoreListResult.StorageFailed("[redacted-store]"),
        )
        val vault = FakeCredentialVault()

        assertIs<CredentialServiceRevealPasswordResult.MissingProfile>(
            service(missingStore, vault).revealPassword(AccountProfileId("profile:v1:missing")),
        )
        val failure = assertIs<CredentialServiceRevealPasswordResult.ProfileStoreFailure>(
            service(failedStore, vault).revealPassword(profile.profileId),
        )

        assertEquals("[redacted-store]", failure.message)
        assertEquals(0, vault.resolveCalls)
    }

    @Test
    fun `reveal password maps vault resolve failures without exposing secrets`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault(
            resolveResult = CredentialVaultResolveResult.CryptoFailed("[redacted-vault]"),
        )
        val service = service(store, vault)

        val failure = assertIs<CredentialServiceRevealPasswordResult.VaultFailure>(
            service.revealPassword(profile.profileId),
        )

        assertEquals("[redacted-vault]", failure.message)
        assertEquals(1, vault.resolveCalls)
    }

    @Test
    fun `delete profiles deletes vault material before deleting profiles`() {
        val first = profile("one")
        val second = profile("two")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(first, second)))
        val vault = FakeCredentialVault()
        val service = service(store, vault)

        val result = assertIs<CredentialServiceDeleteResult.Deleted>(
            service.deleteProfiles(setOf(first.profileId, second.profileId)),
        )

        assertEquals(setOf(first.profileId, second.profileId), result.profileIds)
        assertEquals(listOf(first.credentialHandle, second.credentialHandle), vault.deletedHandles)
        assertEquals(listOf(first.profileId, second.profileId), store.deletedProfileIds)
    }

    @Test
    fun `failed vault delete prevents profile delete`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault(deleteResult = CredentialVaultDeleteResult.StorageFailed("[redacted]"))
        val service = service(store, vault)

        assertIs<CredentialServiceDeleteResult.VaultFailure>(service.deleteProfiles(setOf(profile.profileId)))

        assertEquals(listOf(profile.credentialHandle), vault.deletedHandles)
        assertTrue(store.deletedProfileIds.isEmpty())
    }

    @Test
    fun `missing vault material permits repair delete`() {
        val profile = profile("one")
        val store = FakeAccountProfileStore(listResult = AccountProfileStoreListResult.Listed(listOf(profile)))
        val vault = FakeCredentialVault(
            deleteResult = CredentialVaultDeleteResult.Missing(profile.credentialHandle, "[redacted]"),
        )
        val service = service(store, vault)

        val result = assertIs<CredentialServiceDeleteResult.Deleted>(
            service.deleteProfiles(setOf(profile.profileId)),
        )

        assertEquals(setOf(profile.profileId), result.profileIds)
        assertEquals(listOf(profile.profileId), store.deletedProfileIds)
    }

    private fun service(
        store: AccountProfileStore,
        vault: CredentialVault,
    ): CredentialService = CredentialService(
        accountProfileStore = store,
        credentialVault = vault,
        accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:generated") },
    )

    private fun profile(id: String): SavedAccountProfile = SavedAccountProfile(
        profileId = AccountProfileId("profile:v1:$id"),
        loginName = loginName("venuehost"),
        label = "venuehost resident",
        credentialHandle = CredentialHandle("credential:v1:$id"),
        startLocation = null,
    )

    private fun material(
        password: String,
        loginUri: SecondLifeLoginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
        startLocation: String? = null,
    ): LoginCredentialMaterial = LoginCredentialMaterial(
        loginUri = loginUri,
        loginName = loginName("venuehost"),
        sharedSecret = requireNotNull(SharedSecret.fromPlainText(password)),
        startLocation = startLocation,
    )

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName
}

private class FakeAccountProfileStore(
    var listResult: AccountProfileStoreListResult = AccountProfileStoreListResult.Listed(emptyList()),
    var saveResult: AccountProfileStoreSaveResult? = null,
    var deleteResult: AccountProfileStoreDeleteResult? = null,
) : AccountProfileStore {
    var listCalls = 0
    val savedProfiles = mutableListOf<SavedAccountProfile>()
    val deletedProfileIds = mutableListOf<AccountProfileId>()

    override fun list(): AccountProfileStoreListResult {
        listCalls += 1
        return listResult
    }

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult {
        savedProfiles += profile
        return saveResult ?: AccountProfileStoreSaveResult.Saved(profile)
    }

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult =
        AccountProfileStoreUpdateResult.Updated(profile)

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult {
        deletedProfileIds += profileId
        return deleteResult ?: AccountProfileStoreDeleteResult.Deleted(profileId)
    }
}

private class FakeCredentialVault(
    var saveResult: CredentialVaultSaveResult = CredentialVaultSaveResult.Saved(CredentialHandle("credential:v1:1")),
    var resolveResult: CredentialVaultResolveResult = CredentialVaultResolveResult.Resolved(material("old-password")),
    var updateResult: CredentialVaultUpdateResult? = null,
    var deleteResult: CredentialVaultDeleteResult? = null,
) : CredentialVault {
    val savedMaterials = mutableListOf<LoginCredentialMaterial>()
    val updatedMaterials = mutableListOf<Pair<CredentialHandle, LoginCredentialMaterial>>()
    val deletedHandles = mutableListOf<CredentialHandle>()
    var resolveCalls = 0

    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult {
        savedMaterials += material
        return saveResult
    }

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult {
        updatedMaterials += credentialHandle to material
        return updateResult ?: CredentialVaultUpdateResult.Updated(credentialHandle)
    }

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult {
        deletedHandles += credentialHandle
        return deleteResult ?: CredentialVaultDeleteResult.Deleted(credentialHandle)
    }

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult {
        resolveCalls += 1
        return resolveResult
    }

    companion object {
        private fun material(password: String): LoginCredentialMaterial = LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = assertIs<SecondLifeLoginNameResult.Valid>(
                SecondLifeLoginName.fromUserInput("venuehost"),
            ).loginName,
            sharedSecret = requireNotNull(SharedSecret.fromPlainText(password)),
            startLocation = null,
        )
    }
}
