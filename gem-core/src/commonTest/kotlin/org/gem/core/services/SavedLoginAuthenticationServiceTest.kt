package org.gem.core.services

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AccountProfileId
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
import org.gem.core.testing.failure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SavedLoginAuthenticationServiceTest {
    @Test
    fun `logs in without updating when draft matches saved password`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(resolvePassword = "saved-password")
        val sessionPort = FakeSessionPort()
        val service = service(profile, vault, sessionPort)

        val result = assertIs<SavedLoginAuthenticationResult.Success>(
            service.loginWithSavedProfile(profile.profileId, "saved-password", allowedCompliance()),
        )

        assertEquals(profile, result.profile)
        assertEquals(defaultSession(), result.session)
        assertTrue(vault.updatedPasswords.isEmpty())
        assertEquals(AccountLabel(profile.label), sessionPort.loginRequests.single().accountLabel)
        assertEquals(profile.credentialHandle, sessionPort.loginRequests.single().credentialHandle)
    }

    @Test
    fun `updates changed draft before login and keeps it on success`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(resolvePassword = "old-password")
        val sessionPort = FakeSessionPort()
        val service = service(profile, vault, sessionPort)

        assertIs<SavedLoginAuthenticationResult.Success>(
            service.loginWithSavedProfile(profile.profileId, "new-password", allowedCompliance()),
        )

        assertEquals(listOf("new-password"), vault.updatedPasswords)
        assertEquals(1, sessionPort.loginRequests.size)
    }

    @Test
    fun `normalizes saved password draft and repairs stored paste whitespace before login`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(resolvePassword = "saved-password\n")
        val sessionPort = FakeSessionPort()
        val service = service(profile, vault, sessionPort)

        val result = assertIs<SavedLoginAuthenticationResult.Success>(
            service.loginWithSavedProfile(profile.profileId, " saved-password\n", allowedCompliance()),
        )

        assertEquals(profile, result.profile)
        assertEquals(listOf("saved-password"), vault.updatedPasswords)
        assertEquals(1, sessionPort.loginRequests.size)
    }

    @Test
    fun `restores prior password when changed draft login fails`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(resolvePassword = "old-password")
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                failure(CoreFailureReason.LOGIN_FAILED, "login rejected"),
            ),
        )
        val service = service(profile, vault, sessionPort)

        val result = assertIs<SavedLoginAuthenticationResult.LoginFailed>(
            service.loginWithSavedProfile(profile.profileId, "new-password", allowedCompliance()),
        )

        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals(listOf("new-password", "old-password"), vault.updatedPasswords)
    }

    @Test
    fun `reports restore failure when rollback cannot write prior password`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(
            resolvePassword = "old-password",
            updateResults = mutableListOf(
                CredentialVaultUpdateResult.Updated(profile.credentialHandle),
                CredentialVaultUpdateResult.StorageFailed("[redacted-restore]"),
            ),
        )
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                failure(CoreFailureReason.LOGIN_FAILED, "login rejected"),
            ),
        )
        val service = service(profile, vault, sessionPort)

        val result = assertIs<SavedLoginAuthenticationResult.RestoreFailed>(
            service.loginWithSavedProfile(profile.profileId, "new-password", allowedCompliance()),
        )

        assertEquals(CoreFailureReason.LOGIN_FAILED, result.loginFailure.reason)
        assertEquals("[redacted-restore]", result.restoreMessage)
        assertEquals(listOf("new-password", "old-password"), vault.updatedPasswords)
    }

    @Test
    fun `blank and invalid drafts stop before vault mutation or login`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(resolvePassword = "old-password")
        val sessionPort = FakeSessionPort()
        val service = service(profile, vault, sessionPort)

        val blank = service.loginWithSavedProfile(profile.profileId, " ", allowedCompliance())
        val overlong = service.loginWithSavedProfile(profile.profileId, "12345678901234567", allowedCompliance())

        assertEquals(SavedLoginAuthenticationResult.InvalidPasswordDraft, blank)
        assertEquals(SavedLoginAuthenticationResult.InvalidPasswordDraft, overlong)
        assertEquals(0, vault.resolveCalls)
        assertTrue(vault.updatedPasswords.isEmpty())
        assertTrue(sessionPort.loginRequests.isEmpty())
    }

    @Test
    fun `update failure stops before login`() {
        val profile = profile("one")
        val vault = FakeAuthenticationCredentialVault(
            resolvePassword = "old-password",
            updateResults = mutableListOf(CredentialVaultUpdateResult.CryptoFailed("[redacted-update]")),
        )
        val sessionPort = FakeSessionPort()
        val service = service(profile, vault, sessionPort)

        val result = assertIs<SavedLoginAuthenticationResult.UpdateFailed>(
            service.loginWithSavedProfile(profile.profileId, "new-password", allowedCompliance()),
        )

        assertEquals("[redacted-update]", result.message)
        assertTrue(sessionPort.loginRequests.isEmpty())
    }

    @Test
    fun `reveal failures map to authentication results`() {
        val profile = profile("one")
        val missingService = service(null, FakeAuthenticationCredentialVault(), FakeSessionPort())
        val failedVault = FakeAuthenticationCredentialVault(
            resolveResult = CredentialVaultResolveResult.StorageFailed("[redacted-reveal]"),
        )

        assertIs<SavedLoginAuthenticationResult.MissingProfile>(
            missingService.loginWithSavedProfile(profile.profileId, "password", allowedCompliance()),
        )
        val revealFailed = assertIs<SavedLoginAuthenticationResult.RevealFailed>(
            service(profile, failedVault, FakeSessionPort())
                .loginWithSavedProfile(profile.profileId, "password", allowedCompliance()),
        )

        assertEquals("[redacted-reveal]", revealFailed.message)
    }

    private fun service(
        profile: SavedAccountProfile?,
        vault: FakeAuthenticationCredentialVault,
        sessionPort: FakeSessionPort,
    ): SavedLoginAuthenticationService {
        val profileStore = FakeAuthenticationProfileStore(profile)
        val credentialService = CredentialService(
            accountProfileStore = profileStore,
            credentialVault = vault,
            accountProfileIdSource = AccountProfileIdSource { AccountProfileId("profile:v1:generated") },
        )
        return SavedLoginAuthenticationService(
            credentialService = credentialService,
            sessionService = SessionService(sessionPort, LoginComplianceService(), FakeRedactionPort()),
        )
    }

    private fun profile(id: String): SavedAccountProfile = SavedAccountProfile(
        profileId = AccountProfileId("profile:v1:$id"),
        loginName = loginName("venuehost"),
        label = "venuehost resident",
        credentialHandle = CredentialHandle("credential:v1:$id"),
        startLocation = null,
    )

    private fun material(password: String): LoginCredentialMaterial = LoginCredentialMaterial(
        loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
        loginName = loginName("venuehost"),
        sharedSecret = requireNotNull(SharedSecret.fromPlainText(password)),
        startLocation = null,
    )

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName

    private fun allowedCompliance(): LoginComplianceRequest = LoginComplianceRequest(
        proofAccountAttested = true,
        automatedUse = true,
        scriptedAgentAttested = true,
        operatorLabel = OperatorLabel("operator"),
        proofAccountLabel = "proof-account",
        evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
    )

    private inner class FakeAuthenticationProfileStore(
        private val profile: SavedAccountProfile?,
    ) : AccountProfileStore {
        override fun list(): AccountProfileStoreListResult =
            AccountProfileStoreListResult.Listed(listOfNotNull(profile))

        override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult =
            AccountProfileStoreSaveResult.Saved(profile)

        override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult =
            AccountProfileStoreUpdateResult.Updated(profile)

        override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
            AccountProfileStoreDeleteResult.Deleted(profileId)
    }

    private inner class FakeAuthenticationCredentialVault(
        private val resolvePassword: String = "old-password",
        private val resolveResult: CredentialVaultResolveResult? = null,
        private val updateResults: MutableList<CredentialVaultUpdateResult> = mutableListOf(),
    ) : CredentialVault {
        val updatedPasswords = mutableListOf<String>()
        var resolveCalls = 0

        override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult =
            CredentialVaultSaveResult.Saved(CredentialHandle("credential:v1:generated"))

        override fun update(
            credentialHandle: CredentialHandle,
            material: LoginCredentialMaterial,
        ): CredentialVaultUpdateResult {
            updatedPasswords += material.sharedSecret.revealForLogin()
            return updateResults.removeFirstOrNull()
                ?: CredentialVaultUpdateResult.Updated(credentialHandle)
        }

        override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult =
            CredentialVaultDeleteResult.Deleted(credentialHandle)

        override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult {
            resolveCalls += 1
            return resolveResult ?: CredentialVaultResolveResult.Resolved(material(resolvePassword))
        }
    }
}
