package org.hostess.tools.cli.composition

import java.nio.file.Path
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupSendStatus
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
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
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.DefaultNoticeComplianceClock
import org.hostess.core.services.DefaultRedactionPort
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.services.NoticeComplianceService
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
    fun runtime(mode: CommandMode, noticeLedgerPath: String? = null): CliRuntime = when (mode) {
        CommandMode.FAKE -> fakeRuntime()
        CommandMode.LIVE -> liveRuntime(noticeLedgerPath)
    }

    private fun fakeRuntime(): CliRuntime {
        // Explicit non-live developer mode; never use this runtime as live proof.
        val session = fakeSession()
        val sessionPort = FakeProofSessionPort(session)
        val groupPort = FakeProofGroupPort(fakeGroups)
        val inventoryPort = FakeProofInventoryPort()
        val noticePort = FakeProofNoticePort()
        return CliRuntime(
            sessionService = SessionService(sessionPort, LoginComplianceService(), DefaultRedactionPort),
            groupDirectoryService = GroupDirectoryService(groupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = noticePort,
                clockPort = NoopClockPort,
                noticeComplianceService = NoticeComplianceService(
                    policy = NoticeCompliancePolicy(),
                    ledger = InMemoryNoticeComplianceLedgerPort(),
                    clock = DefaultNoticeComplianceClock(),
                ),
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
            sessionProvider = { session },
        )
    }

    private fun liveRuntime(noticeLedgerPath: String?): CliRuntime {
        val protocolRuntime = ProtocolLibomvModule.liveRuntime()
        val noticeLedger = noticeLedgerPath
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.let { FileNoticeComplianceLedgerPort(Path.of(it)) }
            ?: UnavailableNoticeComplianceLedgerPort()
        return CliRuntime(
            sessionService = SessionService(protocolRuntime.sessionPort, LoginComplianceService(), DefaultRedactionPort),
            groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(protocolRuntime.inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = protocolRuntime.noticePort,
                clockPort = NoopClockPort,
                noticeComplianceService = NoticeComplianceService(
                    policy = NoticeCompliancePolicy(),
                    ledger = noticeLedger,
                    clock = DefaultNoticeComplianceClock(),
                ),
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = protocolRuntime.protocolAvailable,
            sessionProvider = { fakeSession(active = false) },
        )
    }

    companion object {
        fun defaultFakeGroups(): List<GroupMembership> = listOf(
            GroupMembership.fromValues("fake-group-venue", "Venue Hosts", true, true),
            GroupMembership.fromValues("fake-group-events", "Event Notices", true, true),
            GroupMembership.fromValues("fake-group-readonly", "Read Only Group", false, true),
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
    override fun now(): HostessInstant = HostessInstant.EPOCH

    override fun pause(duration: HostessDelay) = Unit
}

private fun fakeSession(active: Boolean = true): HostessSession = HostessSession(
    sessionId = SessionId("fake-session"),
    accountLabel = AccountLabel("fake-account"),
    startedAt = HostessInstant.EPOCH,
    isActive = active,
)
