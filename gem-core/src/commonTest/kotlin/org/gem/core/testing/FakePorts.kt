package org.gem.core.testing

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryAssetId
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemDisplayName
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemKind
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.SessionId
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.ports.ClockPort
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

class FakeSessionPort(
    var loginResult: SessionLoginResult = SessionLoginResult.Success(defaultSession()),
    var logoutResult: SessionLogoutResult = SessionLogoutResult.LoggedOut,
) : SessionPort {
    val loginRequests = mutableListOf<LoginRequest>()
    val logoutRequests = mutableListOf<GemSession>()

    override fun login(request: LoginRequest): SessionLoginResult {
        loginRequests += request
        return loginResult
    }

    override fun logout(session: GemSession): SessionLogoutResult {
        logoutRequests += session
        return logoutResult
    }
}

class FakeAvatarPort(
    var result: AvatarReadinessResult = AvatarReadinessResult.Success(AvatarReadinessProof.success()),
) : AvatarPort {
    val sessions = mutableListOf<GemSession>()

    override fun ensureReady(session: GemSession): AvatarReadinessResult {
        sessions += session
        return result
    }
}

class FakeGroupPort(
    var result: GroupListResult = GroupListResult.Success(emptyList()),
    var presenceResult: SimulatorPresenceProofResult = SimulatorPresenceProofResult.Success(defaultPresenceProof()),
    var archiveResult: GroupNoticeArchiveResult? = null,
) : GroupPort {
    val sessions = mutableListOf<GemSession>()
    val presenceSessions = mutableListOf<GemSession>()
    val archiveRequests = mutableListOf<Pair<GemSession, GroupMembership>>()

    override fun currentGroups(session: GemSession): GroupListResult {
        sessions += session
        return result
    }

    override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult {
        presenceSessions += session
        return presenceResult
    }

    override fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult {
        archiveRequests += session to group
        return archiveResult ?: GroupNoticeArchiveResult.Success(group, listOf(defaultArchiveEntry()))
    }
}

class FakeInventoryPort(
    var existingResult: AttachmentResolutionResult = AttachmentResolutionResult.Resolved(defaultAttachment()),
    var listResult: InventoryItemListResult = InventoryItemListResult.Success(emptyList()),
    var directoryResult: InventoryDirectoryListResult? = null,
) : InventoryPort {
    val existingRequests = mutableListOf<ExistingInventoryAttachment>()
    val listRequests = mutableListOf<InventoryItemQuery>()
    val directoryRequests = mutableListOf<InventoryItemQuery>()

    override fun resolveExistingAttachment(
        session: GemSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult {
        existingRequests += request
        return existingResult
    }

    override fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult {
        listRequests += query
        directoryRequests += query
        return directoryResult ?: listResult.toDirectoryResult()
    }

    private fun InventoryItemListResult.toDirectoryResult(): InventoryDirectoryListResult =
        when (this) {
            is InventoryItemListResult.Success ->
                InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), items))
            is InventoryItemListResult.Failure -> InventoryDirectoryListResult.Failure(failure)
        }
}

class FakeNoticePort(
    private val events: MutableList<String> = mutableListOf(),
    var statesByGroup: Map<GroupId, GroupSendState> = emptyMap(),
) : NoticePort {
    val calls = mutableListOf<NoticeSendCall>()

    override fun sendGroupNotice(
        session: GemSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus {
        calls += NoticeSendCall(session, group, draft, attachment)
        events += "send:${group.groupId.value}"
        return GroupSendStatus(group, statesByGroup[group.groupId] ?: GroupSendState.SENT)
    }
}

data class NoticeSendCall(
    val session: GemSession,
    val group: GroupMembership,
    val draft: NoticeDraft,
    val attachment: AttachmentRef?,
)

class FakeClockPort(
    private val events: MutableList<String> = mutableListOf(),
    private val now: GemInstant = GemInstant.EPOCH,
) : ClockPort {
    val pauses = mutableListOf<GemDelay>()

    override fun now(): GemInstant = now

    override fun pause(duration: GemDelay) {
        pauses += duration
        events += "pause:${duration.milliseconds}ms"
    }
}

class FakeRedactionPort(
    private val redactor: (String) -> String = { "[redacted]" },
) : RedactionPort {
    val inputs = mutableListOf<String>()

    override fun redact(value: String): String {
        inputs += value
        return redactor(value)
    }
}

fun defaultSession(): GemSession = GemSession(
    sessionId = SessionId("session"),
    accountLabel = AccountLabel("proof-account"),
    startedAt = GemInstant.EPOCH,
    isActive = true,
)

fun defaultPresenceProof(): SimulatorPresenceProof = SimulatorPresenceProof(
    simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
    regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
    regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
    agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
    agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
    heartbeatStatus = SimulatorPresenceProofStatus.PASSED,
)

fun defaultArchiveEntry(): GroupNoticeArchiveEntry = GroupNoticeArchiveEntry(
    subject = "Tonight",
    fromName = "proof-account",
    timestamp = 1_717_000_000L,
    hasAttachment = true,
    assetType = 3,
)

fun defaultAttachment(): AttachmentRef = AttachmentRef(
    attachmentId = InventoryItemId("attachment"),
    ownerId = AttachmentOwnerId("owner"),
    kind = AttachmentKind.LANDMARK,
)

fun defaultInventoryItem(): InventoryItemDescriptor = InventoryItemDescriptor(
    itemId = InventoryItemId("landmark"),
    parentFolderId = InventoryFolderId("folder"),
    assetId = InventoryAssetId("asset"),
    displayName = InventoryItemDisplayName("Venue Landmark"),
    kind = InventoryItemKind.LANDMARK,
)

fun failure(reason: CoreFailureReason, message: String): CoreFailure =
    CoreFailure(reason, redactedMessage = message)
