package org.hostess.tools.cli.commands

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
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
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.AvatarReadinessService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.InventorySelectionService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.RecordingCliOutput
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.report.ProofReportWriter

class LiveProofRunnerTest {
    @Test
    fun `simulator presence scope logs in proves presence and logs out without send services`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.SIMULATOR_PRESENCE,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"proofScope\": \"simulator-presence\"")
            assertContains(report, "\"simulatorPresenceStatus\": \"passed\"")
            assertContains(report, "\"regionHandshakeStatus\": \"passed\"")
            assertContains(report, "\"regionHandshakeReplyStatus\": \"passed\"")
            assertContains(report, "\"agentMovementStatus\": \"passed\"")
            assertContains(report, "\"agentUpdateStatus\": \"passed\"")
            assertContains(report, "\"detail\": \"pingReplies=0\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.groupPort.simulatorPresenceCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
            assertFalse(RAW_UUID.containsMatchIn(report))
        }
    }

    @Test
    fun `simulator presence scope logs out after proof gap and stays read only`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    presenceResult = SimulatorPresenceProofResult.Failure(
                        proof = presenceProof().copy(
                            simulatorPresenceStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                            agentMovementStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                            agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
                            redactedMessage = "simulator presence proof_gap",
                        ),
                        failure = CoreFailure(
                            CoreFailureReason.GROUP_LIST_FAILED,
                            "simulator presence proof_gap",
                        ),
                    ),
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.SIMULATOR_PRESENCE,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"proof_gap\"")
            assertContains(report, "\"simulatorPresenceStatus\": \"proof_gap\"")
            assertContains(report, "\"agentMovementStatus\": \"proof_gap\"")
            assertContains(report, "\"agentUpdateStatus\": \"not_run\"")
            assertContains(report, "simulator presence proof_gap")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.groupPort.simulatorPresenceCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `notice archive scope reads archive per selected target and does not send`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.NOTICE_ARCHIVE,
                    targets = listOf("Venue Hosts", "Event Notices"),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                    existingAttachmentName = null,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"proofScope\": \"notice-archive\"")
            assertContains(report, "\"currentGroupsStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveTargetCount\": \"2\"")
            assertContains(report, "\"noticeArchiveMatchedTargetCount\": \"0\"")
            assertContains(report, "\"detail\": \"target=Venue Hosts; entries=1; bodyEcho=not_run\"")
            assertContains(report, "\"detail\": \"target=Event Notices; entries=1; bodyEcho=not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"inventoryCatalogueStatus\": \"not_run\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.groupPort.simulatorPresenceCalls)
            assertEquals(listOf("Venue Hosts", "Event Notices"), ports.groupPort.noticeArchiveGroups.map { it.displayName.value })
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
            assertFalse(RAW_UUID.containsMatchIn(report))
        }
    }

    @Test
    fun `notice archive scope logs out after archive proof gap and stays read only`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    archiveResult = { group ->
                        GroupNoticeArchiveResult.Failure(
                            group = group,
                            failure = CoreFailure(
                                CoreFailureReason.GROUP_LIST_FAILED,
                                "notice archive proof_gap",
                            ),
                        )
                    },
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.NOTICE_ARCHIVE,
                    targets = listOf("Venue Hosts"),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                    existingAttachmentName = null,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"proof_gap\"")
            assertContains(report, "\"noticeArchiveStatus\": \"proof_gap\"")
            assertContains(report, "notice archive proof_gap")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.groupPort.noticeArchiveGroups.size)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `read groups scope logs in reads groups and logs out without send services`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.READ_GROUPS,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"proofScope\": \"read-groups\"")
            assertContains(report, "\"cr\\u0065dentialStatus\": \"passed\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"passed\"")
            assertContains(report, "\"currentGroupsStatus\": \"passed\"")
            assertContains(report, "\"detail\": \"groups=2; displayNames=Event Notices|Venue Hosts\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertFalse(RAW_UUID.containsMatchIn(report))
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `read groups scope logs out after current groups failure`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    GroupListResult.Failure(
                        CoreFailure(
                            CoreFailureReason.GROUP_LIST_FAILED,
                            "current groups transport packet failed: bounded simulator send failed",
                        ),
                    ),
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.READ_GROUPS,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"transport_gap\"")
            assertContains(report, "\"currentGroupsStatus\": \"transport_gap\"")
            assertContains(report, "current groups transport packet failed")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `login only scope logs in logs out and does not read groups or send`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.LOGIN_ONLY,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"proofScope\": \"login-only\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"passed\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `login only scope blocks on login failure without logout groups or send`() {
        withReport { reportPath ->
            val ports = Ports(
                sessionPort = RecordingSessionPort(
                    loginResult = SessionLoginResult.Failure(
                        CoreFailure(CoreFailureReason.LOGIN_FAILED, "login transport unavailable"),
                    ),
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.LOGIN_ONLY,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"proofScope\": \"login-only\"")
            assertContains(report, "\"loginStatus\": \"blocked\"")
            assertContains(report, "\"logoutStatus\": \"not_run\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"step\": \"logout\"")
            assertContains(report, "\"detail\": \"login blocked\"")
            assertEquals(1, Regex("\"step\": \"current-groups\"").findAll(report).count())
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(0, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `inventory catalogue scope logs in lists landmarks and logs out without send services`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(
                    inventoryItem("Venue Landmark", "landmark-b"),
                    inventoryItem("Alpha Landmark", "landmark-a"),
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.INVENTORY_CATALOGUE,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"proofScope\": \"inventory-catalogue\"")
            assertContains(report, "\"loginComplianceStatus\": \"passed\"")
            assertContains(report, "\"loginStatus\": \"passed\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"inventoryCatalogueStatus\": \"passed\"")
            assertContains(report, "\"inventoryItemCount\": \"2\"")
            assertContains(report, "\"detail\": \"items=2; displayNames=Alpha Landmark|Venue Landmark\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"step\": \"cleanup\"")
            assertContains(report, "\"detail\": \"inventory-catalogue scope\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(1, ports.inventoryPort.listQueries.size)
            assertEquals(setOf(InventoryItemKind.LANDMARK), ports.inventoryPort.listQueries.single().kinds)
            assertEquals(0, ports.noticePort.groups.size)
            assertFalse(RAW_UUID.containsMatchIn(report))
        }
    }

    @Test
    fun `inventory catalogue scope logs out and stays read only after inventory failure`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.inventoryPort.listResult = InventoryItemListResult.Failure(
                CoreFailure(
                    CoreFailureReason.INVENTORY_LIST_FAILED,
                    "inventory runtime unavailable: fetch failed",
                ),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    proofScope = LiveProofScope.INVENTORY_CATALOGUE,
                    targets = emptyList(),
                    subject = null,
                    body = null,
                    authorisedLiveSend = false,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"runtime_gap\"")
            assertContains(report, "\"inventoryCatalogueStatus\": \"runtime_gap\"")
            assertContains(report, "inventory runtime unavailable")
            assertContains(report, "\"inventoryItemCount\": \"0\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(1, ports.inventoryPort.listQueries.size)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    private fun runner(
        runtime: CliRuntime,
        reportPath: java.nio.file.Path,
        inputs: LiveProofInputs,
    ): LiveProofRunner = LiveProofRunner(
        runtime = runtime,
        inputs = inputs,
        reportPath = reportPath.toString(),
        output = RecordingCliOutput(),
        commandName = "live-proof",
    )

    private fun inputs(
        proofScope: LiveProofScope = LiveProofScope.FULL,
        targets: List<String> = listOf("Venue Hosts"),
        subject: String? = "Tonight",
        body: String? = "Doors at eight",
        authorisedLiveSend: Boolean = true,
        existingAttachmentName: String? = "Venue Landmark",
    ): LiveProofInputs = LiveProofInputs(
        proofScope = proofScope,
        grid = "second-life",
        account = "venue-proof",
        credentialHandle = "HOSTESS_PROOF_CREDENTIAL",
        credentialFile = null,
        proofAccountAttested = true,
        scriptedAgentAttested = true,
        automatedUse = true,
        operator = "test-operator",
        proofAccountLabel = "test-proof-account",
        targetDisplayNames = targets,
        subject = subject,
        body = body,
        authorisedLiveSend = authorisedLiveSend,
        operatorObservationReady = false,
        existingAttachmentName = existingAttachmentName,
    )

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("hostess-live-proof-runner")
        try {
            assertion(directory.resolve("live-proof.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun inventoryItem(
        displayName: String,
        itemId: String,
        copyable: Boolean? = true,
    ): InventoryItemDescriptor = InventoryItemDescriptor(
        itemId = InventoryItemId(itemId),
        parentFolderId = InventoryFolderId("folder-$itemId"),
        assetId = InventoryAssetId("asset-$itemId"),
        displayName = InventoryItemDisplayName(displayName),
        kind = InventoryItemKind.LANDMARK,
        copyable = copyable,
    )

    private class Ports(
        val sessionPort: RecordingSessionPort = RecordingSessionPort(),
        val groupPort: RecordingGroupPort = RecordingGroupPort(),
        val inventoryPort: RecordingInventoryPort = RecordingInventoryPort(),
        val noticePort: RecordingNoticePort = RecordingNoticePort(),
        private val clock: RecordingClockPort = RecordingClockPort(),
    ) {
        fun runtime(): CliRuntime = CliRuntime(
            sessionService = SessionService(sessionPort, LoginComplianceService(), Redactor),
            avatarReadinessService = AvatarReadinessService(FakeAvatarPort),
            groupDirectoryService = GroupDirectoryService(groupPort),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            inventorySelectionService = InventorySelectionService(),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = noticePort,
                clockPort = clock,
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
        )
    }

    private object FakeAvatarPort : AvatarPort {
        override fun ensureReady(session: HostessSession): AvatarReadinessResult =
            AvatarReadinessResult.Success(AvatarReadinessProof.success())
    }

    private class RecordingSessionPort(
        private val loginResult: SessionLoginResult = SessionLoginResult.Success(SESSION),
        private val logoutResult: SessionLogoutResult = SessionLogoutResult.LoggedOut,
    ) : SessionPort {
        var loginCalls = 0
        var logoutCalls = 0

        override fun login(request: LoginRequest): SessionLoginResult {
            loginCalls += 1
            return loginResult
        }

        override fun logout(session: HostessSession): SessionLogoutResult {
            logoutCalls += 1
            return logoutResult
        }
    }

    private class RecordingGroupPort(
        private val result: GroupListResult = GroupListResult.Success(
            listOf(
                GroupMembership.fromValues("venue-hosts", "Venue Hosts", true, true),
                GroupMembership.fromValues("event-notices", "Event Notices", true, true),
            ),
        ),
        private val presenceResult: SimulatorPresenceProofResult = SimulatorPresenceProofResult.Success(
            presenceProof(),
        ),
        private val archiveResult: (GroupMembership) -> GroupNoticeArchiveResult = { group ->
            GroupNoticeArchiveResult.Success(group, listOf(archiveEntry()))
        },
    ) : GroupPort {
        var currentGroupsCalls = 0
        var simulatorPresenceCalls = 0
        val noticeArchiveGroups = mutableListOf<GroupMembership>()

        override fun currentGroups(session: HostessSession): GroupListResult {
            currentGroupsCalls += 1
            return result
        }

        override fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult {
            simulatorPresenceCalls += 1
            return presenceResult
        }

        override fun noticeArchive(session: HostessSession, group: GroupMembership): GroupNoticeArchiveResult {
            noticeArchiveGroups += group
            return archiveResult(group)
        }
    }

    private class RecordingNoticePort : NoticePort {
        val groups = mutableListOf<GroupMembership>()
        val attachments = mutableListOf<AttachmentRef?>()

        override fun sendGroupNotice(
            session: HostessSession,
            group: GroupMembership,
            draft: NoticeDraft,
            attachment: AttachmentRef?,
        ): GroupSendStatus {
            groups += group
            attachments += attachment
            return GroupSendStatus(group, GroupSendState.SENT)
        }
    }

    private class RecordingClockPort : ClockPort {
        val pauses = mutableListOf<HostessDelay>()

        override fun now(): HostessInstant = HostessInstant.EPOCH

        override fun pause(duration: HostessDelay) {
            pauses += duration
        }
    }

    private class RecordingInventoryPort : InventoryPort {
        var calls = 0
        var listResult: InventoryItemListResult = InventoryItemListResult.Success(emptyList())
        val listQueries = mutableListOf<InventoryItemQuery>()

        override fun resolveExistingAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.ExistingInventoryAttachment,
        ): AttachmentResolutionResult {
            calls += 1
            return AttachmentResolutionResult.Resolved(fakeAttachment(request.kind))
        }

        override fun listDirectory(
            session: HostessSession,
            query: InventoryItemQuery,
        ): InventoryDirectoryListResult {
            listQueries += query
            return when (val result = listResult) {
                is InventoryItemListResult.Success ->
                    InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), result.items))
                is InventoryItemListResult.Failure -> InventoryDirectoryListResult.Failure(result.failure)
            }
        }

        private fun fakeAttachment(kind: AttachmentKind): AttachmentRef = AttachmentRef(
            attachmentId = InventoryItemId("attachment-${kind.name.lowercase()}"),
            ownerId = AttachmentOwnerId("owner"),
            kind = kind,
        )
    }

    private object Redactor : RedactionPort {
        override fun redact(value: String): String = "[redacted]"
    }

    private companion object {
        val SESSION: HostessSession = HostessSession(
            sessionId = SessionId("session"),
            accountLabel = AccountLabel("venue-proof"),
            startedAt = HostessInstant.EPOCH,
            isActive = true,
        )
        val RAW_UUID = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        )

        fun presenceProof(): SimulatorPresenceProof = SimulatorPresenceProof(
            simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
            agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
            agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
        )

        fun archiveEntry(): GroupNoticeArchiveEntry = GroupNoticeArchiveEntry(
            subject = "Tonight",
            fromName = "venue-proof",
            timestamp = 1_717_000_000L,
            hasAttachment = true,
            assetType = 3,
        )
    }
}
