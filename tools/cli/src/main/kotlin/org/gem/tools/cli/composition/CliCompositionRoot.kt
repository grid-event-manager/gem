package org.gem.tools.cli.composition

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupSendStatus
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.SessionId
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveEntry
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryPort
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.NoticePort
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.core.ports.SessionPort
import org.gem.core.ports.SimulatorPresenceProof
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.ports.SimulatorPresenceProofStatus
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.DefaultRedactionPort
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.protocol.libomv.ProtocolLibomvModule
import org.gem.tools.cli.CommandMode
import org.gem.tools.cli.report.ProofReportWriter

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
        val avatarPort = FakeProofAvatarPort()
        return CliRuntime(
            sessionService = SessionService(sessionPort, LoginComplianceService(), DefaultRedactionPort),
            avatarReadinessService = AvatarReadinessService(avatarPort),
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
            avatarReadinessService = AvatarReadinessService(protocolRuntime.avatarPort),
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
            loginStartLocationProbe = LoginStartLocationProbe { handle ->
                protocolRuntime.loginStartLocationProbe.startLocation(handle)
            },
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
    val avatarReadinessService: AvatarReadinessService,
    val groupDirectoryService: GroupDirectoryService,
    val inventoryDirectoryService: InventoryDirectoryService,
    val inventorySelectionService: InventorySelectionService,
    val targetSelectionService: TargetSelectionService,
    val noticeDraftService: NoticeDraftService,
    val attachmentService: AttachmentService,
    val noticeDispatchService: NoticeDispatchService,
    val proofReportWriter: ProofReportWriter,
    val protocolAvailable: Boolean,
    val loginStartLocationProbe: LoginStartLocationProbe = LoginStartLocationProbe.unavailable(),
)

fun interface LoginStartLocationProbe {
    fun startLocation(handle: CredentialHandle): String?

    companion object {
        fun unavailable(): LoginStartLocationProbe = LoginStartLocationProbe { null }
    }
}

private class FakeProofSessionPort(
    private val session: GemSession,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult = SessionLoginResult.Success(session)

    override fun logout(session: GemSession): SessionLogoutResult = SessionLogoutResult.LoggedOut
}

private class FakeProofGroupPort(
    private val groups: List<GroupMembership>,
) : GroupPort {
    override fun currentGroups(session: GemSession): GroupListResult = GroupListResult.Success(groups)

    override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult =
        SimulatorPresenceProofResult.Success(
            SimulatorPresenceProof(
                simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
                regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
                agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
                agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
                heartbeatStatus = SimulatorPresenceProofStatus.PASSED,
            ),
        )

    override fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult =
        GroupNoticeArchiveResult.Success(
            group = group,
            entries = listOf(
                GroupNoticeArchiveEntry(
                    subject = "Fake Notice",
                    fromName = "fake-proof",
                    timestamp = 1L,
                    hasAttachment = true,
                    assetType = 3,
                ),
            ),
        )
}

private class FakeProofAvatarPort : AvatarPort {
    override fun ensureReady(session: GemSession): AvatarReadinessResult =
        AvatarReadinessResult.Success(AvatarReadinessProof.success())
}

private class FakeProofInventoryPort : InventoryPort {
    override fun resolveExistingAttachment(
        session: GemSession,
        request: org.gem.core.domain.ExistingInventoryAttachment,
    ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(
        AttachmentRef(request.itemId, AttachmentOwnerId("fake-owner"), request.kind),
    )

    override fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult =
        InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), emptyList()))
}

private class FakeProofNoticePort : NoticePort {
    override fun sendGroupNotice(
        session: GemSession,
        group: GroupMembership,
        draft: org.gem.core.domain.NoticeDraft,
        attachment: AttachmentRef?,
    ): GroupSendStatus = GroupSendStatus(group, GroupSendState.SENT, "fake mode")
}

private object NoopClockPort : ClockPort {
    override fun now(): GemInstant = GemInstant.EPOCH

    override fun pause(duration: GemDelay) = Unit
}

private fun fakeSession(): GemSession = GemSession(
    sessionId = SessionId("fake-session"),
    accountLabel = AccountLabel("fake-account"),
    startedAt = GemInstant.EPOCH,
    isActive = true,
)
