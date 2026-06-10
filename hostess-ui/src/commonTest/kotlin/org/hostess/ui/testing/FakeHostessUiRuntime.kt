package org.hostess.ui.testing

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryDirectoryListing
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.LoginCredentialMaterial
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.SharedSecret
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.CredentialService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeState
import org.hostess.core.services.HostessCredentialRuntimeUnavailable
import org.hostess.core.services.HostessCredentialRuntimeUnavailableReason
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.core.ports.AccountProfileIdSource
import org.hostess.core.ports.AccountProfileStore
import org.hostess.core.ports.AccountProfileStoreDeleteResult
import org.hostess.core.ports.AccountProfileStoreListResult
import org.hostess.core.ports.AccountProfileStoreSaveResult
import org.hostess.core.ports.AccountProfileStoreUpdateResult
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.CredentialVault
import org.hostess.core.ports.CredentialVaultDeleteResult
import org.hostess.core.ports.CredentialVaultResolveResult
import org.hostess.core.ports.CredentialVaultSaveResult
import org.hostess.core.ports.CredentialVaultUpdateResult
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryDirectoryListResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.RedactionPort
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.ui.runtime.HostessLoginComplianceProvider
import org.hostess.ui.runtime.HostessUiRuntime

object FakeHostessUiRuntime {
    fun ready(profiles: List<SavedAccountProfile> = listOf(defaultProfile())): HostessUiRuntime {
        val profileStore = InMemoryAccountProfileStore(profiles)
        val vault = InMemoryCredentialVault()
        profiles.forEach { profile ->
            vault.materials[profile.credentialHandle] = materialFor(profile.loginName.value)
        }
        val credentialService = CredentialService(
            accountProfileStore = profileStore,
            credentialVault = vault,
            accountProfileIdSource = SequentialAccountProfileIdSource(),
        )
        return runtime(HostessCredentialRuntimeReady(credentialService))
    }

    fun unavailable(): HostessUiRuntime =
        runtime(
            HostessCredentialRuntimeUnavailable(
                reason = HostessCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
                message = "credential runtime unavailable",
            ),
        )

    fun defaultProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:jack"),
            loginName = loginName("jackraybold resident"),
            label = "jackraybold resident",
            credentialHandle = CredentialHandle("hostess-vault:v1:jack"),
            startLocation = null,
        )

    private fun runtime(credentialRuntimeState: HostessCredentialRuntimeState): HostessUiRuntime {
        val sessionPort = FakeSessionPort()
        val inventoryPort = FakeInventoryPort()
        return HostessUiRuntime(
            credentialRuntimeState = credentialRuntimeState,
            sessionService = SessionService(
                sessionPort = sessionPort,
                loginComplianceService = LoginComplianceService(),
                redactionPort = RedactionPort { value -> value },
            ),
            avatarReadinessService = AvatarReadinessService(FakeAvatarPort()),
            groupDirectoryService = GroupDirectoryService(FakeGroupPort()),
            targetSelectionService = TargetSelectionService(),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDraftService = NoticeDraftService(),
            noticeDispatchService = NoticeDispatchService(FakeNoticePort(), FakeClockPort()),
            loginComplianceProvider = HostessLoginComplianceProvider { profile ->
                LoginComplianceRequest(
                    proofAccountAttested = true,
                    automatedUse = true,
                    scriptedAgentAttested = true,
                    operatorLabel = OperatorLabel("Hostess test"),
                    proofAccountLabel = profile.label,
                    evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
                )
            },
        )
    }

    private fun loginName(value: String): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput(value)) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid fake login name")
        }

    private fun materialFor(loginName: String): LoginCredentialMaterial =
        LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName(loginName),
            sharedSecret = SharedSecret.fromPlainText("test-password") ?: error("invalid fake password"),
            startLocation = null,
        )
}

private class SequentialAccountProfileIdSource : AccountProfileIdSource {
    private var next = 0

    override fun nextProfileId(): AccountProfileId {
        next += 1
        return AccountProfileId("profile:v1:$next")
    }
}

private class InMemoryAccountProfileStore(
    profiles: List<SavedAccountProfile>,
) : AccountProfileStore {
    private val profilesById = profiles.associateBy { it.profileId }.toMutableMap()

    override fun list(): AccountProfileStoreListResult =
        AccountProfileStoreListResult.Listed(profilesById.values.toList())

    override fun save(profile: SavedAccountProfile): AccountProfileStoreSaveResult {
        profilesById[profile.profileId] = profile
        return AccountProfileStoreSaveResult.Saved(profile)
    }

    override fun update(profile: SavedAccountProfile): AccountProfileStoreUpdateResult {
        if (!profilesById.containsKey(profile.profileId)) {
            return AccountProfileStoreUpdateResult.Missing(profile.profileId)
        }
        profilesById[profile.profileId] = profile
        return AccountProfileStoreUpdateResult.Updated(profile)
    }

    override fun delete(profileId: AccountProfileId): AccountProfileStoreDeleteResult =
        if (profilesById.remove(profileId) == null) {
            AccountProfileStoreDeleteResult.Missing(profileId)
        } else {
            AccountProfileStoreDeleteResult.Deleted(profileId)
        }
}

private class InMemoryCredentialVault : CredentialVault {
    val materials: MutableMap<CredentialHandle, LoginCredentialMaterial> = linkedMapOf()
    private var next = 0

    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult {
        next += 1
        val handle = CredentialHandle("hostess-vault:v1:fake-$next")
        materials[handle] = material
        return CredentialVaultSaveResult.Saved(handle)
    }

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult {
        if (!materials.containsKey(credentialHandle)) {
            return CredentialVaultUpdateResult.Missing(credentialHandle)
        }
        materials[credentialHandle] = material
        return CredentialVaultUpdateResult.Updated(credentialHandle)
    }

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult =
        if (materials.remove(credentialHandle) == null) {
            CredentialVaultDeleteResult.Missing(credentialHandle)
        } else {
            CredentialVaultDeleteResult.Deleted(credentialHandle)
        }

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult =
        materials[credentialHandle]?.let(CredentialVaultResolveResult::Resolved)
            ?: CredentialVaultResolveResult.Missing(credentialHandle)
}

private class FakeSessionPort : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        SessionLoginResult.Success(
            HostessSession(
                sessionId = SessionId("session"),
                accountLabel = request.accountLabel,
                startedAt = HostessInstant.EPOCH,
                isActive = true,
            ),
        )

    override fun logout(session: HostessSession): SessionLogoutResult =
        SessionLogoutResult.LoggedOut
}

private class FakeAvatarPort : org.hostess.core.ports.AvatarPort {
    override fun ensureReady(session: HostessSession): AvatarReadinessResult =
        AvatarReadinessResult.Success(AvatarReadinessProof.success())
}

private class FakeGroupPort : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult =
        GroupListResult.Success(emptyList())

    override fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult =
        SimulatorPresenceProofResult.Success(
            SimulatorPresenceProof(
                simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
                agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
            ),
        )

    override fun noticeArchive(
        session: HostessSession,
        group: GroupMembership,
    ): GroupNoticeArchiveResult =
        GroupNoticeArchiveResult.Success(group, emptyList())
}

private class FakeInventoryPort : InventoryPort {
    override fun resolveExistingAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        AttachmentResolutionResult.Resolved(
            AttachmentRef(
                attachmentId = request.itemId,
                ownerId = org.hostess.core.domain.AttachmentOwnerId("owner"),
                kind = request.kind,
            ),
        )

    override fun listDirectory(
        session: HostessSession,
        query: org.hostess.core.domain.InventoryItemQuery,
    ): InventoryDirectoryListResult =
        InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), emptyList()))

    override fun listItems(
        session: HostessSession,
        query: org.hostess.core.domain.InventoryItemQuery,
    ): InventoryItemListResult =
        InventoryItemListResult.Success(emptyList())
}

private class FakeNoticePort : NoticePort {
    override fun sendGroupNotice(
        session: HostessSession,
        group: GroupMembership,
        draft: org.hostess.core.domain.NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus =
        GroupSendStatus(group, GroupSendState.SENT)
}

private class FakeClockPort : ClockPort {
    override fun now(): HostessInstant = HostessInstant.EPOCH

    override fun pause(duration: org.hostess.core.domain.HostessDelay) = Unit
}
