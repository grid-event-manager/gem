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
        targets: List<String> = listOf("Venue Hosts"),
        textureFileName: String? = null,
        texturePayloadHandle: String? = null,
        textureDigest: String? = null,
        bulkLimit: Int? = null,
        bulkDelayMs: Long? = null,
        cleanupMode: String? = "delete-created",
        retentionNote: String? = null,
    ): LiveProofInputs = LiveProofInputs(
        grid = "second-life",
        account = "venue-proof",
        credentialHandle = "HOSTESS_PROOF_CREDENTIAL",
        targetDisplayNames = targets,
        subject = "Tonight",
        body = "Doors at eight",
        authorisedLiveSend = true,
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
        val noticePort: RecordingNoticePort = RecordingNoticePort(),
        private val clock: RecordingClockPort = RecordingClockPort(),
    ) {
        fun runtime(): CliRuntime = CliRuntime(
            sessionService = SessionService(FakeSessionPort, Redactor),
            groupDirectoryService = GroupDirectoryService(FakeGroupPort),
            targetSelectionService = TargetSelectionService(),
            noticeDraftService = NoticeDraftService(),
            attachmentService = AttachmentService(FakeInventoryPort),
            noticeDispatchService = NoticeDispatchService(noticePort, clock),
            proofReportWriter = ProofReportWriter(),
            protocolAvailable = true,
            sessionProvider = { SESSION },
        )
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

    private object FakeSessionPort : SessionPort {
        override fun login(request: LoginRequest): SessionLoginResult = SessionLoginResult.Success(SESSION)

        override fun logout(session: HostessSession): SessionLogoutResult = SessionLogoutResult.LoggedOut
    }

    private object FakeGroupPort : GroupPort {
        override fun currentGroups(session: HostessSession): GroupListResult = GroupListResult.Success(
            listOf(
                GroupMembership.fromValues("venue-hosts", "Venue Hosts", true, true),
                GroupMembership.fromValues("event-notices", "Event Notices", true, true),
            ),
        )
    }

    private object FakeInventoryPort : InventoryPort {
        override fun resolveExistingAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.ExistingInventoryAttachment,
        ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(fakeAttachment(request.kind))

        override fun createLandmarkAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.CreateLandmarkAttachment,
        ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.LANDMARK))

        override fun uploadTextureAttachment(
            session: HostessSession,
            request: org.hostess.core.domain.UploadTextureAttachment,
        ): AttachmentResolutionResult = AttachmentResolutionResult.Resolved(fakeAttachment(AttachmentKind.TEXTURE))

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
