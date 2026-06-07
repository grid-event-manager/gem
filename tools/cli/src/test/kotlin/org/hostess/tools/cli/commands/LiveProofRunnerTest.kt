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
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemDisplayName
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.RedactionPort
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.GroupDirectoryService
import org.hostess.core.services.InventoryDirectoryService
import org.hostess.core.services.LoginComplianceService
import org.hostess.core.services.NoticeComplianceClock
import org.hostess.core.services.NoticeComplianceService
import org.hostess.core.services.NoticeDispatchService
import org.hostess.core.services.NoticeDraftService
import org.hostess.core.services.SessionService
import org.hostess.core.services.TargetSelectionService
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.RecordingCliOutput
import org.hostess.tools.cli.composition.CliRuntime
import org.hostess.tools.cli.composition.InMemoryNoticeComplianceLedgerPort
import org.hostess.tools.cli.report.ProofReportWriter

class LiveProofRunnerTest {
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
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"existingAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
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
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
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
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"existingAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
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
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"existingAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
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
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"existingAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.calls)
            assertEquals(1, ports.inventoryPort.listQueries.size)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `plain notice uses core dispatch and missing existing attachment stays blocked`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertEquals(listOf<AttachmentRef?>(null), ports.noticePort.attachments)
            assertContains(report, "\"plainNoticeStatus\": \"passed\"")
            assertContains(report, "\"existingAttachmentStatus\": \"blocked\"")
            assertContains(report, "\"bulkNoticeStatus\": \"blocked\"")
            assertFalse(RAW_UUID.containsMatchIn(report))
        }
    }

    @Test
    fun `repeat targets allow bounded bulk proof through core pacing`() {
        withReport { reportPath ->
            val clock = RecordingClockPort()
            val ports = Ports(clock = clock)

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    targets = listOf("Venue Hosts", "Event Notices"),
                    bulkLimit = 2,
                    bulkDelayMs = 25,
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"bulkNoticeStatus\": \"passed\"")
            assertEquals(listOf(HostessDelay.ofMilliseconds(25)), clock.pauses)
            assertEquals(4, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `retained cleanup without retention note fails overall`() {
        withReport { reportPath ->
            val exit = runner(
                Ports().runtime(),
                reportPath,
                inputs(cleanupMode = "retain-authorised", retentionNote = null),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"failed\"")
            assertContains(report, "\"cleanupStatus\": \"failed\"")
            assertContains(report, "\"step\": \"cleanup\"")
            assertContains(report, "retention note required")
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
        bulkLimit: Int? = null,
        bulkDelayMs: Long? = null,
        cleanupMode: String? = "delete-created",
        retentionNote: String? = null,
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
        existingAttachmentKind = null,
        existingAttachmentId = null,
        bulkLimit = bulkLimit,
        bulkDelayMs = bulkDelayMs,
        cleanupMode = cleanupMode,
        retentionNote = retentionNote,
        recipientCountValues = targets.map { "$it=1" },
        recipientCountSource = "operator-acknowledged",
        noticeLedgerPath = "configured-test-ledger",
    )

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("hostess-live-proof-runner")
        try {
            assertion(directory.resolve("live-proof.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun inventoryItem(displayName: String, itemId: String): InventoryItemDescriptor = InventoryItemDescriptor(
        itemId = InventoryItemId(itemId),
        parentFolderId = InventoryFolderId("folder-$itemId"),
        assetId = InventoryAssetId("asset-$itemId"),
        displayName = InventoryItemDisplayName(displayName),
        kind = InventoryItemKind.LANDMARK,
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
            groupDirectoryService = GroupDirectoryService(groupPort),
            inventoryDirectoryService = InventoryDirectoryService(inventoryPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(
                noticePort = noticePort,
                clockPort = clock,
                noticeComplianceService = NoticeComplianceService(
                    policy = NoticeCompliancePolicy(),
                    ledger = InMemoryNoticeComplianceLedgerPort(),
                    clock = NoticeComplianceClock { NoticeDeliveryDay("2026-06-05") },
                ),
            ),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
        )
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
    ) : GroupPort {
        var currentGroupsCalls = 0

        override fun currentGroups(session: HostessSession): GroupListResult {
            currentGroupsCalls += 1
            return result
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

        override fun listItems(
            session: HostessSession,
            query: InventoryItemQuery,
        ): InventoryItemListResult {
            listQueries += query
            return listResult
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
    }
}
