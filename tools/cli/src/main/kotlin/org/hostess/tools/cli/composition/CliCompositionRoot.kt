package org.hostess.tools.cli.composition

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
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.DefaultRedactionPort
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.LoginComplianceService
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
        // Explicit non-live developer mode; never use this runtime as live proof.
        val session = fakeSession()
        val sessionPort = FakeProofSessionPort(session)
        val groupPort = FakeProofGroupPort(fakeGroups)
        val inventoryPort = FakeProofInventoryPort()
        val noticePort = FakeProofNoticePort()
        return CliRuntime(
            sessionService = SessionService(sessionPort, LoginComplianceService(), DefaultRedactionPort),
            groupDirectoryService = GroupDirectoryService(groupPort),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = noticePort,
                clockPort = NoopClockPort,
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
        )
    }

    private fun liveRuntime(): CliRuntime {
        val protocolRuntime = ProtocolLibomvModule.liveRuntime()
        return CliRuntime(
            sessionService = SessionService(protocolRuntime.sessionPort, LoginComplianceService(), DefaultRedactionPort),
            groupDirectoryService = GroupDirectoryService(protocolRuntime.groupPort),
            inventoryDirectoryService = InventoryDirectoryService(protocolRuntime.inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(protocolRuntime.inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = protocolRuntime.noticePort,
                clockPort = NoopClockPort,
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = protocolRuntime.protocolAvailable,
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
    val inventoryDirectoryService: InventoryDirectoryService,
    val inventorySelectionService: InventorySelectionService,
    val targetSelectionService: TargetSelectionService,
    val noticeDraftService: NoticeDraftService,
    val attachmentService: AttachmentService,
    val noticeDispatchService: NoticeDispatchService,
    val proofReportWriter: ProofReportWriter,
    val protocolAvailable: Boolean,
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

    override fun listItems(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult = InventoryItemListResult.Success(emptyList())
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

private fun fakeSession(): HostessSession = HostessSession(
    sessionId = SessionId("fake-session"),
    accountLabel = AccountLabel("fake-account"),
    startedAt = HostessInstant.EPOCH,
    isActive = true,
)
