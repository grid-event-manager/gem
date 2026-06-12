package org.gem.core.services

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.domain.SecondLifeLoginName
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
import org.gem.core.ports.SessionLoginResult
import org.gem.core.testing.FakeRedactionPort
import org.gem.core.testing.FakeSessionPort
import org.gem.core.testing.defaultSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginProfileAuthenticationServiceTest {
    @Test
    fun `saved profile success delegates through saved authentication service`() {
        val profile = profile("saved")
        val store = FakeProfileStore(initialProfiles = listOf(profile))
        val vault = FakeVault(initialMaterials = mapOf(profile.credentialHandle to material("test-password")))
        val sessionPort = FakeSessionPort()
        val service = service(store, vault, sessionPort)

        val result = assertIs<LoginProfileAuthenticationResult.Success>(
            service.loginSavedProfile(profile.profileId, "test-password", compliance()),
        )

        assertEquals(profile, result.profile)
        assertEquals(defaultSession(), result.session)
        assertEquals(profile.credentialHandle, sessionPort.loginRequests.single().credentialHandle)
    }

    @Test
    fun `new profile success saves profile then authenticates through saved path`() {
        val store = FakeProfileStore()
        val vault = FakeVault()
        val sessionPort = FakeSessionPort()
        val service = service(store, vault, sessionPort)

        val result = assertIs<LoginProfileAuthenticationResult.Success>(
            service.loginNewProfile("newhost", "new-password", compliance()),
        )

        assertEquals("newhost resident", result.profile.loginName.value)
        assertEquals(listOf(result.profile.profileId), store.savedProfiles.map { it.profileId })
        assertTrue(store.deletedProfileIds.isEmpty())
        assertEquals(1, sessionPort.loginRequests.size)
    }

    @Test
    fun `new profile authentication failure deletes saved profile before returning auth failure`() {
        val store = FakeProfileStore()
        val vault = FakeVault()
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, "login rejected"),
            ),
        )
        val service = service(store, vault, sessionPort)

        val result = assertIs<LoginProfileAuthenticationResult.AuthenticationFailed>(
            service.loginNewProfile("newhost", "new-password", compliance()),
        )

        assertEquals("[redacted]", result.message)
        assertEquals(listOf(AccountProfileId("profile:v1:generated")), store.deletedProfileIds)
        assertEquals(listOf(CredentialHandle("credential:v1:1")), vault.deletedHandles)
    }

    @Test
    fun `new profile authentication failure treats already missing rollback profile as auth failure`() {
        val store = FakeProfileStore(hideProfilesOnDeleteList = true)
        val vault = FakeVault()
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, "login rejected"),
            ),
        )
        val service = service(store, vault, sessionPort)

        val result = assertIs<LoginProfileAuthenticationResult.AuthenticationFailed>(
            service.loginNewProfile("newhost", "new-password", compliance()),
        )

        assertEquals("[redacted]", result.message)
        assertTrue(store.deletedProfileIds.isEmpty())
    }

    @Test
    fun `new profile authentication failure reports rollback storage failure`() {
        val store = FakeProfileStore()
        val vault = FakeVault(deleteResult = CredentialVaultDeleteResult.StorageFailed("[redacted-delete]"))
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, "login rejected"),
            ),
        )
        val service = service(store, vault, sessionPort)

        val result = assertIs<LoginProfileAuthenticationResult.NewProfileRollbackFailed>(
            service.loginNewProfile("newhost", "new-password", compliance()),
        )

        assertEquals("[redacted]", result.authenticationMessage)
        assertEquals("[redacted-delete]", result.rollbackMessage)
        assertEquals(listOf(CredentialHandle("credential:v1:1")), vault.deletedHandles)
        assertTrue(store.deletedProfileIds.isEmpty())
    }

    private fun service(
        store: FakeProfileStore,
        vault: FakeVault,
        sessionPort: FakeSessionPort,
    ): LoginProfileAuthenticationService {
        val credentialService = CredentialService(
            accountProfileStore = store,
            credentialVault = vault,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:generated") },
        )
        val savedAuthentication = SavedLoginAuthenticationService(
            credentialService = credentialService,
            sessionService = SessionService(sessionPort, LoginComplianceService(), FakeRedactionPort()),
        )
        return LoginProfileAuthenticationService(credentialService, savedAuthentication)
    }

    private fun profile(id: String): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:$id"),
            loginName = loginName("venuehost"),
            label = "venuehost resident",
            credentialHandle = CredentialHandle("credential:v1:$id"),
            startLocation = null,
        )

    private fun material(password: String): LoginCredentialMaterial =
        LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName("venuehost"),
            sharedSecret = requireNotNull(SharedSecret.fromPlainText(password)),
            startLocation = null,
        )

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName

    private fun compliance(): LoginComplianceRequest =
        LoginComplianceRequest(
            proofAccountAttested = true,
            automatedUse = true,
            scriptedAgentAttested = true,
            operatorLabel = OperatorLabel("operator"),
            proofAccountLabel = "proof-account",
            evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
        )

    private class FakeProfileStore(
        initialProfiles: List<SavedAccountProfile> = emptyList(),
        private val hideProfilesOnDeleteList: Boolean = false,
    ) : AccountProfileStore {
        private val profilesById = initialProfiles.associateBy { it.profileId }.toMutableMap()
        private var listCalls = 0
        val savedProfiles = mutableListOf<SavedAccountProfile>()
        val deletedProfileIds = mutableListOf<AccountProfileId>()

        override fun list(): AccountProfileStoreListResult {
            listCalls += 1
            if (hideProfilesOnDeleteList && listCalls > 1) {
                return AccountProfileStoreListResult.Listed(emptyList())
            }
            return AccountProfileStoreListResult.Listed(profilesById.values.toList())
        }

        override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult {
            savedProfiles += profile
            profilesById[profile.profileId] = profile
            return AccountProfileStoreSaveResult.Saved(profile)
        }

        override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult {
            profilesById[profile.profileId] = profile
            return AccountProfileStoreUpdateResult.Updated(profile)
        }

        override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult {
            deletedProfileIds += profileId
            profilesById.remove(profileId)
            return AccountProfileStoreDeleteResult.Deleted(profileId)
        }
    }

    private class FakeVault(
        initialMaterials: Map<CredentialHandle, LoginCredentialMaterial> = emptyMap(),
        private val deleteResult: CredentialVaultDeleteResult? = null,
    ) : CredentialVault {
        private val materialsByHandle = initialMaterials.toMutableMap()
        private var next = 0
        val deletedHandles = mutableListOf<CredentialHandle>()

        override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult {
            next += 1
            val handle = CredentialHandle("credential:v1:$next")
            materialsByHandle[handle] = material
            return CredentialVaultSaveResult.Saved(handle)
        }

        override fun update(
            credentialHandle: CredentialHandle,
            material: LoginCredentialMaterial,
        ): CredentialVaultUpdateResult {
            materialsByHandle[credentialHandle] = material
            return CredentialVaultUpdateResult.Updated(credentialHandle)
        }

        override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult {
            deletedHandles += credentialHandle
            deleteResult?.let { return it }
            materialsByHandle.remove(credentialHandle)
            return CredentialVaultDeleteResult.Deleted(credentialHandle)
        }

        override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult =
            materialsByHandle[credentialHandle]?.let(CredentialVaultResolveResult::Resolved)
                ?: CredentialVaultResolveResult.Missing(credentialHandle)
    }
}
