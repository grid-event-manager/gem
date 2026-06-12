package org.gem.ui.testing

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SessionId
import org.gem.core.domain.SharedSecret
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.CredentialService
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.services.GemCredentialRuntimeState
import org.gem.core.services.GemCredentialRuntimeUnavailable
import org.gem.core.services.GemCredentialRuntimeUnavailableReason
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.core.theme.ThemePreferenceService
import org.gem.core.ports.AccountProfileIdSource
import org.gem.core.ports.AccountProfileStore
import org.gem.core.ports.AccountProfileStoreDeleteResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.ports.AccountProfileStoreSaveResult
import org.gem.core.ports.AccountProfileStoreUpdateResult
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.CredentialVault
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveEntry
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.ports.InventoryPort
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.NoticePort
import org.gem.core.ports.RedactionPort
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.core.ports.SessionPort
import org.gem.core.ports.SimulatorPresenceProof
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.ports.SimulatorPresenceProofStatus
import org.gem.core.preferences.LastLoginProfilePreferenceLoadResult
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.preferences.LastLoginProfilePreferenceStore
import org.gem.ui.runtime.GemLoginComplianceProvider
import org.gem.ui.runtime.GemUiRuntime

object FakeGemUiRuntime {
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
        noticeArchiveEntriesByGroupId: Map<GroupId, List<GroupNoticeArchiveEntry>>? = null,
        noticeArchiveFailuresByGroupId: Map<GroupId, CoreFailure> = emptyMap(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
        lastLoginProfilePreferenceStore: FakeLastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(),
    ): GemUiRuntime {
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
            credentialRuntimeState = GemCredentialRuntimeReady(credentialService),
            loginSucceeds = loginSucceeds,
            avatarReady = avatarReady,
            avatarRegionName = avatarRegionName,
            groups = groups,
            groupListSucceeds = groupListSucceeds,
            inventoryListing = inventoryListing,
            inventoryListSucceeds = inventoryListSucceeds,
            attachmentSucceeds = attachmentSucceeds,
            noticeRecorder = noticeRecorder,
            noticeArchiveEntriesByGroupId = noticeArchiveEntriesByGroupId,
            noticeArchiveFailuresByGroupId = noticeArchiveFailuresByGroupId,
            themePreferenceStore = themePreferenceStore,
            lastLoginProfilePreferenceStore = lastLoginProfilePreferenceStore,
        )
    }

    fun unavailable(): GemUiRuntime =
        runtime(
            GemCredentialRuntimeUnavailable(
                reason = GemCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
                message = "credential runtime unavailable",
            ),
        )

    fun defaultProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:jack"),
            loginName = loginName("venuehost resident"),
            label = "venuehost resident",
            credentialHandle = CredentialHandle("gem-vault:v1:jack"),
            startLocation = null,
        )

    private fun runtime(
        credentialRuntimeState: GemCredentialRuntimeState,
        loginSucceeds: Boolean = true,
        avatarReady: Boolean = true,
        avatarRegionName: String? = "London City",
        groups: List<GroupMembership> = emptyList(),
        groupListSucceeds: Boolean = true,
        inventoryListing: InventoryDirectoryListing = InventoryDirectoryListing(emptyList(), emptyList()),
        inventoryListSucceeds: Boolean = true,
        attachmentSucceeds: Boolean = true,
        noticeRecorder: FakeNoticeRecorder = FakeNoticeRecorder(),
        noticeArchiveEntriesByGroupId: Map<GroupId, List<GroupNoticeArchiveEntry>>? = null,
        noticeArchiveFailuresByGroupId: Map<GroupId, CoreFailure> = emptyMap(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
        lastLoginProfilePreferenceStore: FakeLastLoginProfilePreferenceStore = FakeLastLoginProfilePreferenceStore(),
    ): GemUiRuntime {
        val sessionPort = FakeSessionPort(loginSucceeds)
        val inventoryPort = FakeInventoryPort(inventoryListing, inventoryListSucceeds, attachmentSucceeds)
        val groupDirectoryService = GroupDirectoryService(
            FakeGroupPort(
                groups = groups,
                groupListSucceeds = groupListSucceeds,
                noticeArchiveEntriesByGroupId = noticeArchiveEntriesByGroupId,
                noticeArchiveFailuresByGroupId = noticeArchiveFailuresByGroupId,
                noticeArchiveSubject = { noticeRecorder.lastSubject },
            ),
        )
        return GemUiRuntime(
            credentialRuntimeState = credentialRuntimeState,
            clockPort = FakeClockPort(),
            sessionService = SessionService(
                sessionPort = sessionPort,
                loginComplianceService = LoginComplianceService(),
                redactionPort = RedactionPort { value -> value },
            ),
            avatarReadinessService = AvatarReadinessService(FakeAvatarPort(avatarReady, avatarRegionName)),
            groupDirectoryService = groupDirectoryService,
            targetSelectionService = TargetSelectionService(),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDraftService = NoticeDraftService(),
            noticeDispatchService = NoticeDispatchService(FakeNoticePort(noticeRecorder), FakeClockPort()),
            noticeConfirmationService = org.gem.core.services.NoticeConfirmationService(groupDirectoryService),
            loginComplianceProvider = GemLoginComplianceProvider { profile ->
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
    private var recordedSubject: String? = null

    val sendCallCount: Int
        get() = sentGroups.size

    val sentGroupDisplayNames: List<String>
        get() = sentGroups.toList()

    val lastSubject: String?
        get() = recordedSubject

    fun record(
        group: GroupMembership,
        draft: org.gem.core.domain.NoticeDraft,
    ): GroupSendStatus {
        val state = scriptedStates.getOrElse(sentGroups.size) { GroupSendState.SENT }
        recordedSubject = draft.subject
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
        val handle = CredentialHandle("gem-vault:v1:fake-$next")
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
                GemSession(
                    sessionId = SessionId("session"),
                    accountLabel = request.accountLabel,
                    startedAt = GemInstant.EPOCH,
                    isActive = true,
                ),
            )
        } else {
            SessionLoginResult.Failure(CoreFailure(CoreFailureReason.LOGIN_FAILED, "login failed"))
        }

    override fun logout(session: GemSession): SessionLogoutResult =
        SessionLogoutResult.LoggedOut
}

private class FakeAvatarPort(
    private val avatarReady: Boolean,
    private val regionName: String?,
) : org.gem.core.ports.AvatarPort {
    override fun ensureReady(session: GemSession): AvatarReadinessResult =
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
    private val noticeArchiveEntriesByGroupId: Map<GroupId, List<GroupNoticeArchiveEntry>>?,
    private val noticeArchiveFailuresByGroupId: Map<GroupId, CoreFailure>,
    private val noticeArchiveSubject: () -> String?,
) : GroupPort {
    override fun currentGroups(session: GemSession): GroupListResult =
        if (groupListSucceeds) {
            GroupListResult.Success(groups)
        } else {
            GroupListResult.Failure(CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, "group list failed"))
        }

    override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult =
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
        session: GemSession,
        group: GroupMembership,
    ): GroupNoticeArchiveResult =
        noticeArchiveFailuresByGroupId[group.groupId]?.let { failure ->
            GroupNoticeArchiveResult.Failure(group, failure)
        } ?: GroupNoticeArchiveResult.Success(group, archiveEntries(group))

    private fun archiveEntries(group: GroupMembership): List<GroupNoticeArchiveEntry> =
        if (noticeArchiveEntriesByGroupId != null) {
            noticeArchiveEntriesByGroupId[group.groupId].orEmpty()
        } else {
            noticeArchiveSubject()
                ?.takeIf(String::isNotBlank)
                ?.let { subject ->
                    listOf(
                        GroupNoticeArchiveEntry(
                            subject = subject,
                            fromName = "venuehost resident",
                            timestamp = 1_717_000_000L,
                            hasAttachment = true,
                            assetType = 3,
                        ),
                    )
                }
                ?: emptyList()
        }
}

private class FakeInventoryPort(
    private val inventoryListing: InventoryDirectoryListing,
    private val inventoryListSucceeds: Boolean,
    private val attachmentSucceeds: Boolean,
) : InventoryPort {
    override fun resolveExistingAttachment(
        session: GemSession,
        request: org.gem.core.domain.ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        if (attachmentSucceeds) {
            AttachmentResolutionResult.Resolved(
                AttachmentRef(
                    attachmentId = request.itemId,
                    ownerId = org.gem.core.domain.AttachmentOwnerId("owner"),
                    kind = request.kind,
                ),
            )
        } else {
            AttachmentResolutionResult.Failed(
                CoreFailure(CoreFailureReason.ATTACHMENT_NOT_FOUND, "attachment resolution failed"),
            )
        }

    override fun listDirectory(
        session: GemSession,
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
        session: GemSession,
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
        session: GemSession,
        group: GroupMembership,
        draft: org.gem.core.domain.NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus =
        recorder.record(group, draft)
}

private class FakeClockPort : ClockPort {
    override fun now(): GemInstant = GemInstant.EPOCH

    override fun pause(duration: org.gem.core.domain.GemDelay) = Unit
}
