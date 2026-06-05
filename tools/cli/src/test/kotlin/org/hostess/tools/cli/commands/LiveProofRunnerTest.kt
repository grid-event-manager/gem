package org.hostess.tools.cli.commands

import java.nio.file.Files
import java.time.Duration
import java.time.Instant
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
import org.hostess.core.services.AttachmentService
import org.hostess.core.services.GroupDirectoryService
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
            assertContains(report, "\"loginStatus\": \"passed\"")
            assertContains(report, "\"currentGroupsStatus\": \"passed\"")
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertContains(report, "\"landmarkAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"textureAttachmentStatus\": \"not_run\"")
            assertContains(report, "\"bulkNoticeStatus\": \"not_run\"")
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
                        CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, "current groups transport unavailable"),
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
            assertContains(report, "\"logoutStatus\": \"passed\"")
            assertContains(report, "\"plainNoticeStatus\": \"not_run\"")
            assertEquals(1, ports.sessionPort.logoutCalls)
            assertEquals(0, ports.noticePort.groups.size)
        }
    }

    @Test
    fun `plain notice uses core dispatch and missing kind fixtures stay blocked`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(ports.runtime(), reportPath, inputs()).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertEquals(listOf<AttachmentRef?>(null), ports.noticePort.attachments)
            assertContains(report, "\"plainNoticeStatus\": \"passed\"")
            assertContains(report, "\"landmarkAttachmentStatus\": \"blocked\"")
            assertContains(report, "\"textureAttachmentStatus\": \"blocked\"")
            assertContains(report, "\"bulkNoticeStatus\": \"blocked\"")
            assertFalse(RAW_UUID.containsMatchIn(report))
        }
    }

    @Test
    fun `texture fixture can pass while landmark fixture remains blocked`() {
        withReport { reportPath ->
            val ports = Ports()

            val exit = runner(
                ports.runtime(),
                reportPath,
                inputs(
                    textureFileName = "poster.png",
                    texturePayloadHandle = "opaque-texture-handle",
                    textureDigest = "sha256:abc",
                ),
            ).run()

            val report = reportPath.readText()
            assertEquals(CommandResult.UNAVAILABLE, exit)
            assertContains(report, "\"landmarkAttachmentStatus\": \"blocked\"")
            assertContains(report, "\"textureAttachmentStatus\": \"passed\"")
            assertEquals(AttachmentKind.TEXTURE, ports.noticePort.attachments.filterNotNull().single().kind)
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
            assertEquals(listOf(Duration.ofMillis(25)), clock.pauses)
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
        textureFileName: String? = null,
        texturePayloadHandle: String? = null,
        textureDigest: String? = null,
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
        targetDisplayNames = targets,
        subject = subject,
        body = body,
        authorisedLiveSend = authorisedLiveSend,
        existingAttachmentKind = null,
        existingAttachmentId = null,
        landmarkVenue = null,
        landmarkRegionId = null,
        landmarkLocalPosition = null,
        textureFileName = textureFileName,
        texturePayloadHandle = texturePayloadHandle,
        textureDigest = textureDigest,
        bulkLimit = bulkLimit,
        bulkDelayMs = bulkDelayMs,
        cleanupMode = cleanupMode,
        retentionNote = retentionNote,
    )

    private fun withReport(assertion: (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("hostess-live-proof-runner")
        try {
            assertion(directory.resolve("live-proof.json"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class Ports(
        val sessionPort: RecordingSessionPort = RecordingSessionPort(),
        val groupPort: RecordingGroupPort = RecordingGroupPort(),
        val inventoryPort: RecordingInventoryPort = RecordingInventoryPort(),
        val noticePort: RecordingNoticePort = RecordingNoticePort(),
        private val clock: RecordingClockPort = RecordingClockPort(),
    ) {
        fun runtime(): CliRuntime = CliRuntime(
            sessionService = SessionService(sessionPort, Redactor),
            groupDirectoryService = GroupDirectoryService(groupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(inventoryPort),
            noticeDispatchService = NoticeDispatchService(noticePort, clock),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
            sessionProvider = { SESSION },
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
        val pauses = mutableListOf<Duration>()

        override fun now(): Instant = Instant.EPOCH

        override fun pause(duration: Duration) {
            pauses += duration
        }
    }

    private class RecordingInventoryPort : InventoryPort {
        var calls = 0

        override fun resolveExistingAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.ExistingInventoryAttachment,
        ): AttachmentResolutionResult {
            calls += 1
            return AttachmentResolutionResult.Resolved(fakeAttachment(request.kind))
        }

        override fun createLandmarkAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.CreateLandmarkAttachment,
        ): AttachmentResolutionResult {
            calls += 1
            return AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.LANDMARK))
        }

        override fun uploadTextureAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.UploadTextureAttachment,
        ): AttachmentResolutionResult {
            calls += 1
            return AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.TEXTURE))
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
            startedAt = Instant.EPOCH,
            isActive = true,
        )
        val RAW_UUID = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        )
    }
}
