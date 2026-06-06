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
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.RedactionPort
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort

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

class FakeGroupPort(
    var result: GroupListResult = GroupListResult.Success(emptyList()),
) : GroupPort {
    val sessions = mutableListOf<HostessSession>()

    override fun currentGroups(session: HostessSession): GroupListResult {
        sessions += session
        return result
    }
}

class FakeInventoryPort(
    var existingResult: AttachmentResolutionResult = AttachmentResolutionResult.Resolved(defaultAttachment()),
) : InventoryPort {
    val existingRequests = mutableListOf<ExistingInventoryAttachment>()

    override fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult {
        existingRequests += request
        return existingResult
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

fun defaultAttachment(): AttachmentRef = AttachmentRef(
    attachmentId = InventoryItemId("attachment"),
    ownerId = AttachmentOwnerId("owner"),
    kind = AttachmentKind.LANDMARK,
)

fun failure(reason: CoreFailureReason, message: String): CoreFailure =
    CoreFailure(reason, redactedMessage = message)
