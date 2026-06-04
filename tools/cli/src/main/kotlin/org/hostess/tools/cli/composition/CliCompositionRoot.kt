package org.hostess.tools.cli.composition

import java.time.Duration
import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
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
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.protocol.libomv.ProtocolLibomvModule
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.report.ProofReportWriter

class CliCompositionRoot(
    private val fakeGroups: List<GroupMembership> = defaultFakeGroups(),
) {
    fun runtime(mode: CommandMode): CliRuntime = when (mode) {
        CommandMode.FAKE -> fakeRuntime()
        CommandMode.LIVE -> liveRuntime()
    }

    private fun fakeRuntime(): CliRuntime {
        val session = fakeSession()
        val sessionPort = FakeProofSessionPort(session)
        val groupPort = FakeProofGroupPort(fakeGroups)
        val inventoryPort = FakeProofInventoryPort()
        val noticePort = FakeProofNoticePort()
        return CliRuntime(
            sessionService = SessionService(sessionPort, CliRedactionPort),
            groupDirectoryService = GroupDirectoryService(groupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(noticePort, NoopClockPort),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
            sessionProvider = { session },
        )
    }

    private fun liveRuntime(): CliRuntime {
        val protocolRuntime = ProtocolLibomvModule.liveRuntime()
        return CliRuntime(
            sessionService = SessionService(protocolRuntime.sessionPort, CliRedactionPort),
            groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(protocolRuntime.inventoryPort),
            noticeDispatchService = NoticeDispatchService(protocolRuntime.noticePort, NoopClockPort),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = protocolRuntime.protocolAvailable,
            sessionProvider = { fakeSession(active = false) },
        )
    }

    companion object {
        fun defaultFakeGroups(): List<GroupMembership> = listOf(
            GroupMembership(GroupId("fake-group-venue"), GroupDisplayName("Venue Hosts"), true, true),
            GroupMembership(GroupId("fake-group-events"), GroupDisplayName("Event Notices"), true, true),
            GroupMembership(GroupId("fake-group-readonly"), GroupDisplayName("Read Only Group"), false, true),
        )
    }
}

data class CliRuntime(
    val sessionService: SessionService,
    val groupDirectoryService: GroupDirectoryService,
    val targetSelectionService: TargetSelectionService,
    val noticeDraftService: NoticeDraftService,
    val attachmentService: AttachmentService,
    val noticeDispatchService: NoticeDispatchService,
    val proofReportWriter: ProofReportWriter,
    val protocolAvailable: Boolean,
    val sessionProvider: () -> HostessSession,
)

private class FakeProofSessionPort(
    private val session: HostessSession,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult = SessionLoginResult.Success(session)

    override fun logout(session: HostessSession): SessionLogoutResult = SessionLogoutResult.LoggedOut
}

private class FakeProofGroupPort(
    private val groups: List<GroupMembership>,
) : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult = GroupListResult.Success(groups)
}

private class FakeProofInventoryPort : InventoryPort {
    override fun resolveExistingAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.ExistingInventoryAttachment,
    ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(
        AttachmentRef(request.itemId, AttachmentOwnerId("fake-owner"), request.kind),
    )

    override fun createLandmarkAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.CreateLandmarkAttachment,
    ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.LANDMARK))

    override fun uploadTextureAttachment(
        session: HostessSession,
        request: org.hostess.core.domain.UploadTextureAttachment,
    ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.TEXTURE))

    private fun fakeAttachment(kind: AttachmentKind): AttachmentRef =
        AttachmentRef(InventoryItemId("fake-attachment-${kind.name.lowercase()}"), AttachmentOwnerId("fake-owner"), kind)
}

private class FakeProofNoticePort : NoticePort {
    override fun sendGroupNotice(
        session: HostessSession,
        group: GroupMembership,
        draft: org.hostess.core.domain.NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus = GroupSendStatus(group, GroupSendState.SENT, "fake mode")
}

private object NoopClockPort : ClockPort {
    override fun now(): Instant = Instant.EPOCH

    override fun pause(duration: Duration) = Unit
}

private object CliRedactionPort : RedactionPort {
    override fun redact(value: String): String = "[redacted]"
}

private fun fakeSession(active: Boolean = true): HostessSession = HostessSession(
    sessionId = SessionId("fake-session"),
    accountLabel = AccountLabel("fake-account"),
    startedAt = Instant.EPOCH,
    isActive = active,
)
