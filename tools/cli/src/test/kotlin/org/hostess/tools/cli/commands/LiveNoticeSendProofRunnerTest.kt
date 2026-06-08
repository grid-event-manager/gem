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
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.AvatarPort
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessProofStatus
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveEntry
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.ports.GroupPort
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

class LiveNoticeSendProofRunnerTest {
    @Test
    fun `full proof sends one display-name landmark notice workflow and logs out`() {
        withReport { reportPath ->
            val groups = listOf(
                GroupMembership.fromValues("owks", "Owks", true, true),
                GroupMembership.fromValues("minx", "m!nx", true, true),
            )
            val ports = Ports(
                groupPort = RecordingGroupPort(
                    result = GroupListResult.Success(groups),
                    presenceResult = failingPresenceResult(),
                ),
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
            assertContains(report, "\"regionProtocolStatus\": \"passed\"")
            assertContains(report, "\"agentAppearanceServiceStatus\": \"passed\"")
            assertContains(report, "\"cofVersionStatus\": \"passed\"")
            assertContains(report, "\"serverAppearanceStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveStatus\": \"passed\"")
            assertContains(report, "\"noticeArchiveTargetCount\": \"2\"")
            assertContains(report, "\"noticeArchiveMatchedTargetCount\": \"2\"")
            assertContains(report, "\"existingAttachmentDisplayName\": \"Venue Landmark\"")
            assertStepOrder(
                report,
                listOf(
                    "validate-inputs",
                    "login",
                    "avatar-readiness",
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
            assertEquals(0, ports.groupPort.simulatorPresenceCalls)
            assertEquals(listOf("Owks", "m!nx"), ports.groupPort.noticeArchiveGroups.map { it.displayName.value })
            assertEquals(setOf(InventoryItemKind.LANDMARK), ports.inventoryPort.listQueries.single().kinds)
            assertFalse(RAW_UUID.containsMatchIn(report))
            assertFalse(report.contains("landmark-item"))
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
        existingAttachmentName = existingAttachmentName,
    )

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("hostess-live-notice-proof")
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
        )
    }

    private class RecordingAvatarPort(
        var result: AvatarReadinessResult = AvatarReadinessResult.Success(AvatarReadinessProof.success()),
    ) : AvatarPort {
        var calls = 0

        override fun ensureReady(session: HostessSession): AvatarReadinessResult =
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

        override fun logout(session: HostessSession): SessionLogoutResult {
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

    private class RecordingNoticePort(
        private val state: GroupSendState = GroupSendState.SENT,
        private val detail: String? = null,
    ) : NoticePort {
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
            return GroupSendStatus(group, state, detail)
        }
    }

    private class RecordingClockPort : ClockPort {
        override fun now(): HostessInstant = HostessInstant.EPOCH

        override fun pause(duration: HostessDelay) = Unit
    }

    private class RecordingInventoryPort : InventoryPort {
        var listResult: InventoryItemListResult = InventoryItemListResult.Success(emptyList())
        var resolveResult: AttachmentResolutionResult? = null
        var resolveRequests = 0
        val listQueries = mutableListOf<InventoryItemQuery>()

        override fun resolveExistingAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.ExistingInventoryAttachment,
        ): AttachmentResolutionResult {
            resolveRequests += 1
            return resolveResult ?: AttachmentResolutionResult.Resolved(
                AttachmentRef(request.itemId, AttachmentOwnerId("owner"), request.kind),
            )
        }

        override fun listItems(
            session: HostessSession,
            query: InventoryItemQuery,
        ): InventoryItemListResult {
            listQueries += query
            return listResult
        }
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

        fun presenceProof(
            simulatorPresenceStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeReplyStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            agentMovementStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            agentUpdateStatus: SimulatorPresenceProofStatus = SimulatorPresenceProofStatus.PASSED,
            message: String? = null,
        ): SimulatorPresenceProof = SimulatorPresenceProof(
            simulatorPresenceStatus = simulatorPresenceStatus,
            regionHandshakeStatus = regionHandshakeStatus,
            regionHandshakeReplyStatus = regionHandshakeReplyStatus,
            agentMovementStatus = agentMovementStatus,
            agentUpdateStatus = agentUpdateStatus,
            redactedMessage = message,
        )

        fun failingPresenceResult(): SimulatorPresenceProofResult = SimulatorPresenceProofResult.Failure(
            proof = presenceProof(
                simulatorPresenceStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                agentMovementStatus = SimulatorPresenceProofStatus.PROOF_GAP,
                agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
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
