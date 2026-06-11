package org.hostess.ui.testing

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryDirectoryListing
import org.hostess.core.domain.InventoryItemQuery
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
import org.hostess.core.theme.ThemePreferenceService
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
import org.hostess.core.preferences.LastLoginProfilePreferenceLoadResult
import org.hostess.core.preferences.LastLoginProfilePreferenceSaveResult
import org.hostess.core.preferences.LastLoginProfilePreferenceService
import org.hostess.core.preferences.LastLoginProfilePreferenceStore
import org.hostess.ui.runtime.HostessLoginComplianceProvider
import org.hostess.ui.runtime.HostessUiRuntime

object FakeHostessUiRuntime {
    fun ready(
        profiles: List<SavedAccountProfile> = listOf(defaultProfile()),
        loginSucceeds: Boolean = true,
        avatarReady: Boolean = true,
        avatarRegionName: String? = "London City",
        groups: List<GroupMembership> = emptyList(),
        groupListSucceeds: Boolean = true,
        inventoryListing: InventoryDirectoryListing = InventoryDirectoryListing(emptyList(), emptyList()),
        inventoryListSucceeds: Boolean = true,
        attachmentSucceeds: Boolean = true,
        noticeRecorder: FakeNoticeRecorder = FakeNoticeRecorder(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
        lastLoginProfilePreferenceStore: FakeLastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(),
    ): HostessUiRuntime {
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
        return runtime(
            credentialRuntimeState = HostessCredentialRuntimeReady(credentialService),
            loginSucceeds = loginSucceeds,
            avatarReady = avatarReady,
            avatarRegionName = avatarRegionName,
            groups = groups,
            groupListSucceeds = groupListSucceeds,
            inventoryListing = inventoryListing,
            inventoryListSucceeds = inventoryListSucceeds,
            attachmentSucceeds = attachmentSucceeds,
            noticeRecorder = noticeRecorder,
            themePreferenceStore = themePreferenceStore,
            lastLoginProfilePreferenceStore = lastLoginProfilePreferenceStore,
        )
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
            loginName = loginName("venuehost resident"),
            label = "venuehost resident",
            credentialHandle = CredentialHandle("hostess-vault:v1:jack"),
            startLocation = null,
        )

    private fun runtime(
        credentialRuntimeState: HostessCredentialRuntimeState,
        loginSucceeds: Boolean = true,
        avatarReady: Boolean = true,
        avatarRegionName: String? = "London City",
        groups: List<GroupMembership> = emptyList(),
        groupListSucceeds: Boolean = true,
        inventoryListing: InventoryDirectoryListing = InventoryDirectoryListing(emptyList(), emptyList()),
        inventoryListSucceeds: Boolean = true,
        attachmentSucceeds: Boolean = true,
        noticeRecorder: FakeNoticeRecorder = FakeNoticeRecorder(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
        lastLoginProfilePreferenceStore: FakeLastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(),
    ): HostessUiRuntime {
        val sessionPort = FakeSessionPort(loginSucceeds)
        val inventoryPort = FakeInventoryPort(inventoryListing, inventoryListSucceeds, attachmentSucceeds)
        return HostessUiRuntime(
            credentialRuntimeState = credentialRuntimeState,
            sessionService = SessionService(
                sessionPort = sessionPort,
                loginComplianceService = LoginComplianceService(),
                redactionPort = RedactionPort { value -> value },
            ),
            avatarReadinessService = AvatarReadinessService(FakeAvatarPort(avatarReady, avatarRegionName)),
            groupDirectoryService = GroupDirectoryService(FakeGroupPort(groups, groupListSucceeds)),
            targetSelectionService = TargetSelectionService(),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDraftService = NoticeDraftService(),
            noticeDispatchService = NoticeDispatchService(FakeNoticePort(noticeRecorder), FakeClockPort()),
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
            themePreferenceService = ThemePreferenceService(themePreferenceStore),
            lastLoginProfilePreferenceService = LastLoginProfilePreferenceService(lastLoginProfilePreferenceStore),
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

class FakeNoticeRecorder(
    private val scriptedStates: List<GroupSendState> = emptyList(),
) {
    private val sentGroups = mutableListOf<String>()

    val sendCallCount: Int
        get() = sentGroups.size

    val sentGroupDisplayNames: List<String>
        get() = sentGroups.toList()

    fun record(group: GroupMembership): GroupSendStatus {
        val state = scriptedStates.getOrElse(sentGroups.size) { GroupSendState.SENT }
        sentGroups += group.displayName.value
        return GroupSendStatus(group, state)
    }
}

class FakeLastLoginProfilePreferenceStore(
    private var profileId: AccountProfileId? = null,
) : LastLoginProfilePreferenceStore {
    val savedProfileIds = mutableListOf<AccountProfileId>()

    override fun load(): LastLoginProfilePreferenceLoadResult =
        profileId
            ?.let(LastLoginProfilePreferenceLoadResult::Loaded)
            ?: LastLoginProfilePreferenceLoadResult.Missing

    override fun save(profileId: AccountProfileId): LastLoginProfilePreferenceSaveResult {
        this.profileId = profileId
        savedProfileIds += profileId
        return LastLoginProfilePreferenceSaveResult.Saved
    }
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

private class FakeSessionPort(
    private val loginSucceeds: Boolean,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        if (loginSucceeds) {
            SessionLoginResult.Success(
                HostessSession(
                    sessionId = SessionId("session"),
                    accountLabel = request.accountLabel,
                    startedAt = HostessInstant.EPOCH,
                    isActive = true,
                ),
            )
        } else {
            SessionLoginResult.Failure(CoreFailure(CoreFailureReason.LOGIN_FAILED, "login failed"))
        }

    override fun logout(session: HostessSession): SessionLogoutResult =
        SessionLogoutResult.LoggedOut
}

private class FakeAvatarPort(
    private val avatarReady: Boolean,
    private val regionName: String?,
) : org.hostess.core.ports.AvatarPort {
    override fun ensureReady(session: HostessSession): AvatarReadinessResult =
        if (avatarReady) {
            AvatarReadinessResult.Success(AvatarReadinessProof.success(regionName = regionName))
        } else {
            AvatarReadinessResult.Failure(
                proof = AvatarReadinessProof.notRun(),
                failure = CoreFailure(CoreFailureReason.AVATAR_READINESS_FAILED, "avatar readiness failed"),
            )
        }
}

private class FakeGroupPort(
    private val groups: List<GroupMembership>,
    private val groupListSucceeds: Boolean,
) : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult =
        if (groupListSucceeds) {
            GroupListResult.Success(groups)
        } else {
            GroupListResult.Failure(CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, "group list failed"))
        }

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

private class FakeInventoryPort(
    private val inventoryListing: InventoryDirectoryListing,
    private val inventoryListSucceeds: Boolean,
    private val attachmentSucceeds: Boolean,
) : InventoryPort {
    override fun resolveExistingAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        if (attachmentSucceeds) {
            AttachmentResolutionResult.Resolved(
                AttachmentRef(
                    attachmentId = request.itemId,
                    ownerId = org.hostess.core.domain.AttachmentOwnerId("owner"),
                    kind = request.kind,
                ),
            )
        } else {
            AttachmentResolutionResult.Failed(
                CoreFailure(CoreFailureReason.ATTACHMENT_NOT_FOUND, "attachment resolution failed"),
            )
        }

    override fun listDirectory(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult =
        if (inventoryListSucceeds) {
            InventoryDirectoryListResult.Success(
                InventoryDirectoryListing(
                    folders = inventoryListing.folders,
                    items = inventoryListing.items.filter { it.kind in query.kinds },
                ),
            )
        } else {
            InventoryDirectoryListResult.Failure(
                CoreFailure(CoreFailureReason.INVENTORY_LIST_FAILED, "inventory listing failed"),
            )
        }

    override fun listItems(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult =
        if (inventoryListSucceeds) {
            InventoryItemListResult.Success(inventoryListing.items.filter { it.kind in query.kinds })
        } else {
            InventoryItemListResult.Failure(
                CoreFailure(CoreFailureReason.INVENTORY_LIST_FAILED, "inventory item listing failed"),
            )
        }
}

private class FakeNoticePort(
    private val recorder: FakeNoticeRecorder,
) : NoticePort {
    override fun sendGroupNotice(
        session: HostessSession,
        group: GroupMembership,
        draft: org.hostess.core.domain.NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus =
        recorder.record(group)
}

private class FakeClockPort : ClockPort {
    override fun now(): HostessInstant = HostessInstant.EPOCH

    override fun pause(duration: org.hostess.core.domain.HostessDelay) = Unit
}
