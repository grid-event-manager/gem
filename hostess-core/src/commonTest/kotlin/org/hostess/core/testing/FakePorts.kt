package org.hostess.core.testing

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryAssetId
import org.hostess.core.domain.InventoryDirectoryListing
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemDisplayName
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.AvatarPort
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveEntry
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

class FakeSessionPort(
    var loginResult: SessionLoginResult = SessionLoginResult.Success(defaultSession()),
    var logoutResult: SessionLogoutResult = SessionLogoutResult.LoggedOut,
) : SessionPort {
    val loginRequests = mutableListOf<LoginRequest>()
    val logoutRequests = mutableListOf<HostessSession>()

    override fun login(request: LoginRequest): SessionLoginResult {
        loginRequests += request
        return loginResult
    }

    override fun logout(session: HostessSession): SessionLogoutResult {
        logoutRequests += session
        return logoutResult
    }
}

class FakeAvatarPort(
    var result: AvatarReadinessResult = AvatarReadinessResult.Success(AvatarReadinessProof.success()),
) : AvatarPort {
    val sessions = mutableListOf<HostessSession>()

    override fun ensureReady(session: HostessSession): AvatarReadinessResult {
        sessions += session
        return result
    }
}

class FakeGroupPort(
    var result: GroupListResult = GroupListResult.Success(emptyList()),
    var presenceResult: SimulatorPresenceProofResult = SimulatorPresenceProofResult.Success(defaultPresenceProof()),
    var archiveResult: GroupNoticeArchiveResult? = null,
) : GroupPort {
    val sessions = mutableListOf<HostessSession>()
    val presenceSessions = mutableListOf<HostessSession>()
    val archiveRequests = mutableListOf<Pair<HostessSession, GroupMembership>>()

    override fun currentGroups(session: HostessSession): GroupListResult {
        sessions += session
        return result
    }

    override fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult {
        presenceSessions += session
        return presenceResult
    }

    override fun noticeArchive(session: HostessSession, group: GroupMembership): GroupNoticeArchiveResult {
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
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult {
        existingRequests += request
        return existingResult
    }

    override fun listDirectory(
        session: HostessSession,
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
        session: HostessSession,
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
    val session: HostessSession,
    val group: GroupMembership,
    val draft: NoticeDraft,
    val attachment: AttachmentRef?,
)

class FakeClockPort(
    private val events: MutableList<String> = mutableListOf(),
    private val now: HostessInstant = HostessInstant.EPOCH,
) : ClockPort {
    val pauses = mutableListOf<HostessDelay>()

    override fun now(): HostessInstant = now

    override fun pause(duration: HostessDelay) {
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

fun defaultSession(): HostessSession = HostessSession(
    sessionId = SessionId("session"),
    accountLabel = AccountLabel("proof-account"),
    startedAt = HostessInstant.EPOCH,
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
