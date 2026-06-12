package org.gem.tools.cli.commands

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
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
import org.gem.core.ports.AvatarReadinessProofStatus
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
import org.gem.core.services.AttachmentService
import org.gem.core.services.AvatarReadinessService
import org.gem.core.services.GroupDirectoryService
import org.gem.core.services.InventoryDirectoryService
import org.gem.core.services.InventorySelectionService
import org.gem.core.services.LoginComplianceService
import org.gem.core.services.NoticeDispatchService
import org.gem.core.services.NoticeDraftService
import org.gem.core.services.SessionService
import org.gem.core.services.TargetSelectionService
import org.gem.tools.cli.CommandResult
import org.gem.tools.cli.RecordingCliOutput
import org.gem.tools.cli.composition.CliRuntime
import org.gem.tools.cli.composition.LoginStartLocationProbe
import org.gem.tools.cli.report.ProofReportWriter

class LiveNoticeSendProofRunnerTest {
    @Test
    fun `full proof sends one display-name landmark notice workflow and logs out`() {
        withReport { reportPath ->
            val groups = listOf(
                GroupMembership.fromValues("owks", "Owks", true, true),
                GroupMembership.fromValues("minx", "m!nx", true, true),
            )
            val ports = Ports(
                groupPort = RecordingGroupPort(result = GroupListResult.Success(groups)),
            )
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(targets = listOf("Owks", "m!nx")),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertEquals(listOf("Owks", "m!nx"), ports.noticePort.groups.map { it.displayName.value })
            assertEquals(1, ports.inventoryPort.resolveRequests)
            assertEquals(2, ports.noticePort.attachments.size)
            assertTrue(ports.noticePort.attachments.all { it?.kind == AttachmentKind.LANDMARK })
            assertEquals(1, Regex("\"step\": \"group-notice\"").findAll(report).count())
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"passed\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"passed\"")
            assertContains(report, "\"noticeSendStatus\": \"passed\"")
            assertContains(report, "\"avatarReadinessStatus\": \"passed\"")
            assertContains(report, "\"simulatorPresenceStatus\": \"passed\"")
            assertContains(report, "\"simulatorSessionStatus\": \"passed\"")
            assertContains(report, "\"simulatorHeartbeatStatus\": \"passed\"")
            assertContains(report, "\"regionProtocolStatus\": \"passed\"")
            assertContains(report, "\"agentAppearanceServiceStatus\": \"passed\"")
            assertContains(report, "\"cofVersionStatus\": \"passed\"")
            assertContains(report, "\"serverAppearanceStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveTargetCount\": \"2\"")
            assertContains(report, "\"noticeArchiveMatchedTargetCount\": \"2\"")
            assertContains(report, "\"simulatorLogoutStatus\": \"passed\"")
            assertContains(report, "\"existingAttachmentDisplayName\": \"Venue Landmark\"")
            assertContains(report, "\"loginStartLocationStatus\": \"passed\"")
            assertContains(report, "\"loginStartLocation\": \"uri:London City&76&174&23\"")
            assertStepOrder(
                report,
                listOf(
                    "validate-inputs",
                    "login-start-location",
                    "login",
                    "avatar-readiness",
                    "simulator-session",
                    "current-groups",
                    "select-targets",
                    "inventory-catalogue",
                    "select-attachment",
                    "resolve-attachment",
                    "group-notice",
                    "notice-archive",
                    "cleanup",
                    "logout",
                ),
            )
            assertFalse(report.contains("\"step\": \"simulator-presence\""))
            assertContains(report, "\"step\": \"select-attachment\"")
            assertContains(report, "\"step\": \"resolve-attachment\"")
            assertContains(report, "\"step\": \"group-notice\"")
            assertContains(report, "\"detail\": \"targets=2; matched=2; entries=2; bodyEcho=not_run\"")
            assertContains(report, "\"cleanupStatus\": \"passed\"")
            assertContains(report, "authorised retained external notice proof")
            assertEquals(1, ports.sessionPort.loginCalls)
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.avatarPort.calls)
            assertEquals(1, ports.groupPort.simulatorPresenceCalls)
            assertEquals(listOf("Owks", "m!nx"), ports.groupPort.noticeArchiveGroups.map { it.displayName.value })
            assertEquals(setOf(InventoryItemKind.LANDMARK), ports.inventoryPort.listQueries.single().kinds)
            assertFalse(RAW_UUID.containsMatchIn(report))
            assertFalse(report.contains("landmark-item"))
        }
    }

    @Test
    fun `simulator heartbeat proof gap logs out before groups inventory or send`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    presenceResult = SimulatorPresenceProofResult.Success(
                        presenceProof(
                            heartbeatStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                            message = "simulator heartbeat proof_gap",
                        ),
                    ),
                ),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"proof_gap\"")
            assertContains(report, "\"simulatorSessionStatus\": \"passed\"")
            assertContains(report, "\"simulatorHeartbeatStatus\": \"proof_gap\"")
            assertContains(report, "\"step\": \"simulator-session\"")
            assertContains(report, "\"state\": \"proof_gap\"")
            assertContains(report, "simulator heartbeat proof_gap")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"simulatorLogoutStatus\": \"passed\"")
            assertEquals(1, ports.groupPort.simulatorPresenceCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.noticePort.groups.size)
            assertEquals(1, ports.sessionPort.logoutCalls)
        }
    }

    @Test
    fun `operator receipt diagnostic is redacted and does not override archive pass`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    operatorReceiptStatus = OperatorReceiptStatus.PARTIAL,
                    operatorReceiptDetail = "Hecate missing; https://secret.example/cap session_id=abc",
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"status\": \"passed\"")
            assertContains(report, "\"noticeArchiveStatus\": \"passed\"")
            assertContains(report, "\"operatorReceiptStatus\": \"partial\"")
            assertContains(report, "\"operatorReceiptDetail\": \"Hecate missing; [redacted-url] session_id=[redacted]\"")
            assertFalse(report.contains("https://secret.example/cap"))
            assertFalse(report.contains("session_id=abc"))
        }
    }

    @Test
    fun `avatar readiness failure logs out before groups inventory or send`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.avatarPort.result = AvatarReadinessResult.Failure(
                proof = avatarProof(
                    avatarReadinessStatus = AvatarReadinessProofStatus.PROOF_GAP,
                    serverAppearanceStatus = AvatarReadinessProofStatus.PROOF_GAP,
                ),
                failure = CoreFailure(
                    CoreFailureReason.AVATAR_READINESS_FAILED,
                    "avatar readiness proof_gap",
                ),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"proof_gap\"")
            assertContains(report, "\"avatarReadinessStatus\": \"proof_gap\"")
            assertContains(report, "\"simulatorPresenceStatus\": \"passed\"")
            assertContains(report, "\"regionProtocolStatus\": \"passed\"")
            assertContains(report, "\"agentAppearanceServiceStatus\": \"passed\"")
            assertContains(report, "\"cofVersionStatus\": \"passed\"")
            assertContains(report, "\"serverAppearanceStatus\": \"proof_gap\"")
            assertContains(report, "\"step\": \"avatar-readiness\"")
            assertContains(report, "\"currentGroupsStatus\": \"not_run\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"noticeArchiveStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertFalse(report.contains("https://secret.example/cap"))
            assertFalse(report.contains("Doors at eight"))
            assertEquals(1, ports.avatarPort.calls)
            assertEquals(0, ports.groupPort.simulatorPresenceCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.listQueries.size)
            assertEquals(0, ports.inventoryPort.resolveRequests)
            assertEquals(0, ports.noticePort.groups.size)
            assertEquals(1, ports.sessionPort.logoutCalls)
        }
    }

    @Test
    fun `forbidden target blocks before login`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(targets = listOf("omnivrz")),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "forbidden target display name")
            assertContains(report, "\"loginStatus\": \"not_run\"")
            assertEquals(0, ports.sessionPort.loginCalls)
            assertEquals(0, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.inventoryPort.listQueries.size)
            assertEquals(0, ports.inventoryPort.resolveRequests)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `second life full proof blocks before login when start location is not London City`() {
        withReport { reportPath ->
            val ports = Ports(loginStartLocation = "last")

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"blocked\"")
            assertContains(report, "\"loginStartLocationStatus\": \"blocked\"")
            assertContains(report, "\"loginStartLocation\": \"last\"")
            assertContains(report, "Agni proof start location uncontrolled")
            assertContains(report, "\"loginStatus\": \"not_run\"")
            assertEquals(0, ports.sessionPort.loginCalls)
            assertEquals(0, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.groupPort.currentGroupsCalls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `current groups failure logs out and does not send`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    GroupListResult.Failure(
                        CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, "current groups transport failed"),
                    ),
                ),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"currentGroupsStatus\": \"transport_gap\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.inventoryPort.resolveRequests)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `inventory failure logs out and does not select attach or send`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.inventoryPort.listResult = InventoryItemListResult.Failure(
                CoreFailure(CoreFailureReason.INVENTORY_LIST_FAILED, "inventory runtime unavailable"),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"inventoryCatalogueStatus\": \"runtime_gap\"")
            assertContains(report, "\"attachmentSelectionStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.inventoryPort.resolveRequests)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `attachment selection failures log out and do not resolve or send`() {
        val cases = listOf(
            "attachment display name unavailable" to emptyList(),
            "attachment display name ambiguous" to listOf(
                inventoryItem("Venue Landmark", "landmark-a", copyable = true),
                inventoryItem("Venue Landmark", "landmark-b", copyable = true),
            ),
            "attachment display name has wrong kind" to listOf(
                inventoryItem("Venue Landmark", "notecard-a", kind = InventoryItemKind.NOTECARD, copyable = true),
            ),
            "attachment is not copyable" to listOf(
                inventoryItem("Venue Landmark", "landmark-no-copy", copyable = false),
            ),
            "attachment copyability unknown" to listOf(
                inventoryItem("Venue Landmark", "landmark-unknown-copy", copyable = null),
            ),
        )

        cases.forEach { (detail, items) ->
            withReport { reportPath ->
                val ports = Ports()
                ports.inventoryPort.listResult = InventoryItemListResult.Success(items)

                val exit = runner(ports.runtime(), reportPath, inputs()).run()

                val report = reportPath.readText()
                assertEquals(CommandResult.UNAVAILABLE, exit)
                assertContains(report, "\"attachmentSelectionStatus\": \"blocked\"")
                assertContains(report, detail)
                assertContains(report, "\"attachmentResolutionStatus\": \"not_run\"")
                assertContains(report, "\"noticeSendStatus\": \"not_run\"")
                assertContains(report, "\"logoutStatus\": \"passed\"")
                assertEquals(1, ports.sessionPort.logoutCalls)
                assertEquals(0, ports.inventoryPort.resolveRequests)
                assertEquals(0, ports.noticePort.groups.size)
            }
        }
    }

    @Test
    fun `attachment resolution failure logs out and does not send`() {
        withReport { reportPath ->
            val ports = Ports()
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )
            ports.inventoryPort.resolveResult = AttachmentResolutionResult.Failed(
                CoreFailure(CoreFailureReason.ATTACHMENT_NOT_FOUND, "attachment runtime unavailable"),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"attachmentSelectionStatus\": \"passed\"")
            assertContains(report, "\"attachmentResolutionStatus\": \"runtime_gap\"")
            assertContains(report, "\"noticeSendStatus\": \"not_run\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.inventoryPort.resolveRequests)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `dispatch failure logs out after one group notice step`() {
        withReport { reportPath ->
            val ports = Ports(noticePort = RecordingNoticePort(state = GroupSendState.FAILED, detail = "transport send failed"))
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"noticeSendStatus\": \"transport_gap\"")
            assertContains(report, "\"step\": \"group-notice\"")
            assertContains(report, "\"noticeArchiveStatus\": \"not_run\"")
            assertContains(report, "\"cleanupStatus\": \"not_applicable\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, Regex("\"step\": \"group-notice\"").findAll(report).count())
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(1, ports.noticePort.groups.size)
            assertEquals(0, ports.groupPort.noticeArchiveGroups.size)
        }
    }

    @Test
    fun `notice archive subject miss fails after dispatch and before cleanup`() {
        withReport { reportPath ->
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    archiveResult = { group ->
                        GroupNoticeArchiveResult.Success(
                            group = group,
                            entries = listOf(
                                GroupNoticeArchiveEntry(
                                    subject = "Earlier",
                                    fromName = "venue-proof",
                                    timestamp = 1_717_000_000L,
                                    hasAttachment = true,
                                    assetType = 3,
                                ),
                            ),
                        )
                    },
                ),
            )
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"proof_gap\"")
            assertContains(report, "\"noticeSendStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveStatus\": \"proof_gap\"")
            assertContains(report, "\"noticeArchiveTargetCount\": \"1\"")
            assertContains(report, "\"noticeArchiveMatchedTargetCount\": \"0\"")
            assertContains(report, "subjectMatch=missing")
            assertContains(report, "\"cleanupStatus\": \"not_applicable\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertEquals(1, ports.noticePort.groups.size)
            assertEquals(List(4) { "Venue Hosts" }, ports.groupPort.noticeArchiveGroups.map { it.displayName.value })
        }
    }

    @Test
    fun `notice archive retries failed post-send read before passing cleanup`() {
        withReport { reportPath ->
            var archiveCalls = 0
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    archiveResult = { group ->
                        archiveCalls += 1
                        if (archiveCalls == 1) {
                            GroupNoticeArchiveResult.Failure(
                                group = group,
                                failure = CoreFailure(
                                    CoreFailureReason.GROUP_LIST_FAILED,
                                    "notice archive proof_gap reply_timeout",
                                ),
                            )
                        } else {
                            GroupNoticeArchiveResult.Success(
                                group = group,
                                entries = listOf(
                                    GroupNoticeArchiveEntry(
                                        subject = "Tonight",
                                        fromName = "venue-proof",
                                        timestamp = 1_717_000_000L,
                                        hasAttachment = true,
                                        assetType = 3,
                                    ),
                                ),
                            )
                        }
                    },
                ),
            )
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.SUCCESS, exit)
            assertContains(report, "\"noticeArchiveStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveMatchedTargetCount\": \"1\"")
            assertContains(report, "\"cleanupStatus\": \"passed\"")
            assertEquals(2, archiveCalls)
            assertEquals(listOf("Venue Hosts", "Venue Hosts"), ports.groupPort.noticeArchiveGroups.map { it.displayName.value })
        }
    }

    @Test
    fun `logout failure makes successful send proof fail`() {
        withReport { reportPath ->
            val ports = Ports(
                sessionPort = RecordingSessionPort(
                    logoutResult = SessionLogoutResult.Failure(
                        CoreFailure(CoreFailureReason.LOGOUT_FAILED, "logout runtime unavailable"),
                    ),
                ),
            )
            ports.inventoryPort.listResult = InventoryItemListResult.Success(
                listOf(inventoryItem("Venue Landmark", "landmark-item", copyable = true)),
            )

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"status\": \"failed\"")
            assertContains(report, "\"noticeSendStatus\": \"passed\"")
            assertContains(report, "\"logoutStatus\": \"failed\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
        }
    }

    private fun runner(
        runtime: CliRuntime,
        reportPath: java.nio.file.Path,
        inputs: LiveProofInputs,
    ): LiveNoticeSendProofRunner = LiveNoticeSendProofRunner(
        runtime = runtime,
        inputs = inputs,
        reportPath = reportPath.toString(),
        output = RecordingCliOutput(),
        commandName = "live-proof",
    )

    private fun inputs(
        targets: List<String> = listOf("Venue Hosts"),
        existingAttachmentName: String? = "Venue Landmark",
        operatorReceiptStatus: OperatorReceiptStatus? = null,
        operatorReceiptDetail: String? = null,
    ): LiveProofInputs = LiveProofInputs(
        proofScope = LiveProofScope.FULL,
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
        subject = "Tonight",
        body = "Doors at eight",
        authorisedLiveSend = true,
        operatorObservationReady = true,
        operatorReceiptStatus = operatorReceiptStatus,
        operatorReceiptDetail = operatorReceiptDetail,
        existingAttachmentName = existingAttachmentName,
    )

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("gem-live-notice-proof")
        try {
            assertion(directory.resolve("live-proof.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun inventoryItem(
        displayName: String,
        itemId: String,
        kind: InventoryItemKind = InventoryItemKind.LANDMARK,
        copyable: Boolean? = true,
    ): InventoryItemDescriptor = InventoryItemDescriptor(
        itemId = InventoryItemId(itemId),
        parentFolderId = InventoryFolderId("folder-$itemId"),
        assetId = InventoryAssetId("asset-$itemId"),
        displayName = InventoryItemDisplayName(displayName),
        kind = kind,
        copyable = copyable,
    )

    private class Ports(
        val sessionPort: RecordingSessionPort = RecordingSessionPort(),
        val groupPort: RecordingGroupPort = RecordingGroupPort(),
        val inventoryPort: RecordingInventoryPort = RecordingInventoryPort(),
        val noticePort: RecordingNoticePort = RecordingNoticePort(),
        val avatarPort: RecordingAvatarPort = RecordingAvatarPort(),
        private val clock: RecordingClockPort = RecordingClockPort(),
        private val loginStartLocation: String? = "uri:London City&76&174&23",
    ) {
        fun runtime(): CliRuntime = CliRuntime(
            sessionService = SessionService(sessionPort, LoginComplianceService(), Redactor),
            avatarReadinessService = AvatarReadinessService(avatarPort),
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
            loginStartLocationProbe = LoginStartLocationProbe { _ -> loginStartLocation },
        )
    }

    private class RecordingAvatarPort(
        var result: AvatarReadinessResult = AvatarReadinessResult.Success(AvatarReadinessProof.success()),
    ) : AvatarPort {
        var calls = 0

        override fun ensureReady(session: GemSession): AvatarReadinessResult =
            result.also { calls += 1 }
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

        override fun logout(session: GemSession): SessionLogoutResult {
            logoutCalls += 1
            return logoutResult
        }
    }

    private class RecordingGroupPort(
        private val result: GroupListResult = GroupListResult.Success(
            listOf(GroupMembership.fromValues("venue-hosts", "Venue Hosts", true, true)),
        ),
        private val presenceResult: SimulatorPresenceProofResult = SimulatorPresenceProofResult.Success(
            presenceProof(),
        ),
        private val archiveResult: (GroupMembership) -> GroupNoticeArchiveResult = { group ->
            GroupNoticeArchiveResult.Success(
                group = group,
                entries = listOf(
                    GroupNoticeArchiveEntry(
                        subject = "Tonight",
                        fromName = "venue-proof",
                        timestamp = 1_717_000_000L,
                        hasAttachment = true,
                        assetType = 3,
                    ),
                ),
            )
        },
    ) : GroupPort {
        var currentGroupsCalls = 0
        var simulatorPresenceCalls = 0
        val noticeArchiveGroups = mutableListOf<GroupMembership>()

        override fun currentGroups(session: GemSession): GroupListResult {
            currentGroupsCalls += 1
            return result
        }

        override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult {
            simulatorPresenceCalls += 1
            return presenceResult
        }

        override fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult {
            noticeArchiveGroups += group
            return archiveResult(group)
        }
    }

    private class RecordingNoticePort(
        private val state: GroupSendState = GroupSendState.SENT,
        private val detail: String? = null,
    ) : NoticePort {
        val groups = mutableListOf<GroupMembership>()
        val attachments = mutableListOf<AttachmentRef?>()

        override fun sendGroupNotice(
            session: GemSession,
            group: GroupMembership,
            draft: NoticeDraft,
            attachment: AttachmentRef?,
        ): GroupSendStatus {
            groups += group
            attachments += attachment
            return GroupSendStatus(group, state, detail)
        }
    }

    private class RecordingClockPort : ClockPort {
        override fun now(): GemInstant = GemInstant.EPOCH

        override fun pause(duration: GemDelay) = Unit
    }

    private class RecordingInventoryPort : InventoryPort {
        var listResult: InventoryItemListResult = InventoryItemListResult.Success(emptyList())
        var resolveResult: AttachmentResolutionResult? = null
        var resolveRequests = 0
        val listQueries = mutableListOf<InventoryItemQuery>()

        override fun resolveExistingAttachment(
            session: GemSession,
            request: org.gem.core.domain.ExistingInventoryAttachment,
        ): AttachmentResolutionResult {
            resolveRequests += 1
            return resolveResult ?: AttachmentResolutionResult.Resolved(
                AttachmentRef(request.itemId, AttachmentOwnerId("owner"), request.kind),
            )
        }

        override fun listDirectory(
            session: GemSession,
            query: InventoryItemQuery,
        ): InventoryDirectoryListResult {
            listQueries += query
            return when (val result = listResult) {
                is InventoryItemListResult.Success ->
                    InventoryDirectoryListResult.Success(InventoryDirectoryListing(emptyList(), result.items))
                is InventoryItemListResult.Failure -> InventoryDirectoryListResult.Failure(result.failure)
            }
        }
    }

    private object Redactor : RedactionPort {
        override fun redact(value: String): String = "[redacted]"
    }

    private companion object {
        val SESSION: GemSession = GemSession(
            sessionId = SessionId("session"),
            accountLabel = AccountLabel("venue-proof"),
            startedAt = GemInstant.EPOCH,
            isActive = true,
        )
        val RAW_UUID = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        )

        fun presenceProof(
            simulatorPresenceStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeReplyStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            agentMovementStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            agentUpdateStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            heartbeatStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            message: String? = null,
        ): SimulatorPresenceProof = SimulatorPresenceProof(
            simulatorPresenceStatus = simulatorPresenceStatus,
            regionHandshakeStatus = regionHandshakeStatus,
            regionHandshakeReplyStatus = regionHandshakeReplyStatus,
            agentMovementStatus = agentMovementStatus,
            agentUpdateStatus = agentUpdateStatus,
            heartbeatStatus = heartbeatStatus,
            redactedMessage = message,
        )

        fun failingPresenceResult(): SimulatorPresenceProofResult = SimulatorPresenceProofResult.Failure(
            proof = presenceProof(
                simulatorPresenceStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                agentMovementStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
                heartbeatStatus = SimulatorPresenceProofStatus.NOT_RUN,
                message = "diagnostic simulator presence proof_gap",
            ),
            failure = CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, "diagnostic simulator presence proof_gap"),
        )

        fun avatarProof(
            avatarReadinessStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
            simulatorPresenceStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
            regionProtocolStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
            agentAppearanceServiceStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
            cofVersionStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
            serverAppearanceStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.PASSED,
        ): AvatarReadinessProof = AvatarReadinessProof(
            avatarReadinessStatus = avatarReadinessStatus,
            simulatorPresenceStatus = simulatorPresenceStatus,
            regionProtocolStatus = regionProtocolStatus,
            agentAppearanceServiceStatus = agentAppearanceServiceStatus,
            cofVersionStatus = cofVersionStatus,
            serverAppearanceStatus = serverAppearanceStatus,
        )

        fun assertStepOrder(report: String, steps: List<String>) {
            var previous = -1
            steps.forEach { step ->
                val index = report.indexOf("\"step\": \"$step\"")
                assertTrue(index > previous, "expected step $step after prior proof step")
                previous = index
            }
        }
    }
}
