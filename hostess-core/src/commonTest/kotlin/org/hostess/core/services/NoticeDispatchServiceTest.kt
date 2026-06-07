package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentOwnerId
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionCount
import org.hostess.core.domain.NoticeSubmissionLedgerSnapshot
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.NoticeSubmissionLedgerPort
import org.hostess.core.testing.FakeClockPort
import org.hostess.core.testing.FakeNoticePort

class NoticeDispatchServiceTest {
    @Test
    fun `dispatch calls notice port once per selected group and records sent groups only`() {
        val events = mutableListOf<String>()
        val ledger = RecordingNoticeSubmissionLedger()
        val noticePort = FakeNoticePort(
            events = events,
            statesByGroup = mapOf(
                GroupId("music") to GroupSendState.SENT,
                GroupId("gallery") to GroupSendState.FAILED,
            ),
        )
        val service = service(noticePort, FakeClockPort(events), ledger)
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(
                session = session(),
                draft = draft,
                compliance = request(),
                pacingPolicy = PacingPolicy(HostessDelay.ofSeconds(5)),
            ),
        ).result

        assertEquals(listOf("send:music", "pause:5000ms", "send:gallery"), events)
        assertEquals(listOf(GroupSendState.SENT, GroupSendState.FAILED), result.statuses.map { it.state })
        assertEquals(
            listOf(RecordCall(projectionGroupIds = listOf("music", "gallery"), sentGroupIds = listOf("music"))),
            ledger.recordCalls,
        )
    }

    @Test
    fun `dispatch rejects invalid draft without calling notice port`() {
        val events = mutableListOf<String>()
        val ledger = RecordingNoticeSubmissionLedger()
        val service = service(FakeNoticePort(events), FakeClockPort(events), ledger)
        val invalidDraft = NoticeDraft(
            subject = "",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.Rejected>(
            service.dispatch(session(), invalidDraft, request()),
        )

        val validation = assertIs<NoticeDraftValidation.Invalid>(result.validation)
        assertTrue(NoticeDraftInvalidReason.BLANK_SUBJECT in validation.reasons)
        assertTrue(events.isEmpty())
        assertEquals(0, ledger.snapshotCalls)
    }

    @Test
    fun `dispatch rejects compliance before calling notice port`() {
        val events = mutableListOf<String>()
        val noticePort = FakeNoticePort(events)
        val service = service(
            noticePort = noticePort,
            clockPort = FakeClockPort(events),
            ledger = RecordingNoticeSubmissionLedger(
                snapshotResult = NoticeComplianceLedgerResult.Success(
                    listOf(snapshot("music", "Music Room", reserved = 180), snapshot("gallery", "Gallery")),
                ),
            ),
        )
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.ComplianceRejected>(
            service.dispatch(session(), draft, request()),
        )

        assertIs<NoticeComplianceDecision.Denied>(result.decision)
        assertEquals("notice_submission_cap_exceeded", result.decision.receipt.reasonCode)
        assertTrue(events.isEmpty())
        assertTrue(noticePort.calls.isEmpty())
    }

    @Test
    fun `dispatch surfaces ledger record failure after sent statuses`() {
        val events = mutableListOf<String>()
        val ledger = RecordingNoticeSubmissionLedger(
            recordResult = NoticeComplianceLedgerResult.Failure("disk unavailable"),
        )
        val noticePort = FakeNoticePort(events)
        val service = service(noticePort, FakeClockPort(events), ledger)
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.ComplianceRecordFailed>(
            service.dispatch(session(), draft, request()),
        )

        assertEquals("ledger_record_failed", result.complianceReceipt.reasonCode)
        assertEquals(listOf(GroupSendState.SENT, GroupSendState.SENT), result.result.statuses.map { it.state })
        assertEquals(
            listOf(RecordCall(projectionGroupIds = listOf("music", "gallery"), sentGroupIds = listOf("music", "gallery"))),
            ledger.recordCalls,
        )
    }

    @Test
    fun `dispatch forwards resolved attachment to each selected group`() {
        val noticePort = FakeNoticePort()
        val service = service(noticePort, FakeClockPort())
        val attachment = AttachmentRef(
            attachmentId = InventoryItemId("landmark-item"),
            ownerId = AttachmentOwnerId("owner"),
            kind = AttachmentKind.LANDMARK,
        )
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        assertIs<NoticeDispatchResult.Sent>(
            service.dispatch(
                session = session(),
                draft = draft,
                compliance = request(),
                attachment = attachment,
            ),
        )

        assertEquals(listOf(attachment, attachment), noticePort.calls.map { it.attachment })
    }

    private fun selectedTargets() = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(
            listOf(
                group("music", "Music Room"),
                group("gallery", "Gallery"),
            ),
        ).addAllSendable(),
    ).targetSet

    private fun group(id: String, displayName: String): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = true,
        acceptsNotices = null,
    )

    private fun session(): HostessSession = HostessSession(
        sessionId = SessionId("session"),
        accountLabel = AccountLabel("proof-account"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private fun service(
        noticePort: FakeNoticePort,
        clockPort: FakeClockPort,
        ledger: NoticeSubmissionLedgerPort = RecordingNoticeSubmissionLedger(),
    ): NoticeDispatchService = NoticeDispatchService(
        noticePort = noticePort,
        clockPort = clockPort,
        noticeComplianceService = NoticeComplianceService(
            policy = NoticeCompliancePolicy(),
            ledger = ledger,
            clock = NoticeComplianceClock { NoticeLedgerDay("2026-06-05") },
        ),
    )

    private fun request(): NoticeComplianceRequest = NoticeComplianceRequest(OperatorLabel("operator"))

    private data class RecordCall(
        val projectionGroupIds: List<String>,
        val sentGroupIds: List<String>,
    )

    private class RecordingNoticeSubmissionLedger(
        private val snapshotResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
        private val reserveResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
        private val recordResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
    ) : NoticeSubmissionLedgerPort {
        var snapshotCalls = 0
        val reserveCalls = mutableListOf<List<String>>()
        val recordCalls = mutableListOf<RecordCall>()

        override fun snapshot(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            groups: List<GroupMembership>,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
            snapshotCalls += 1
            return snapshotResult ?: NoticeComplianceLedgerResult.Success(
                groups.map { group -> snapshot(group.groupId.value, group.displayName.value) },
            )
        }

        override fun reserve(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            projection: NoticeSubmissionProjection,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
            reserveCalls += projection.selectedGroups.map { it.groupId.value }
            return reserveResult ?: NoticeComplianceLedgerResult.Success(
                projection.selectedGroups.map { group -> snapshot(group.groupId.value, group.displayName.value, reserved = 1) },
            )
        }

        override fun recordSendResult(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            projection: NoticeSubmissionProjection,
            sentGroups: List<GroupMembership>,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
            recordCalls += RecordCall(
                projectionGroupIds = projection.selectedGroups.map { it.groupId.value },
                sentGroupIds = sentGroups.map { it.groupId.value },
            )
            return recordResult ?: NoticeComplianceLedgerResult.Success(
                projection.selectedGroups.map { group -> snapshot(group.groupId.value, group.displayName.value) },
            )
        }
    }

    private companion object {
        fun snapshot(
            groupId: String,
            groupName: String,
            reserved: Long = 0,
            sent: Long = 0,
        ): NoticeSubmissionLedgerSnapshot = NoticeSubmissionLedgerSnapshot(
            proofAccountLabel = AccountLabel("proof-account"),
            groupId = GroupId(groupId),
            groupDisplayName = GroupDisplayName(groupName),
            noticeLedgerDay = NoticeLedgerDay("2026-06-05"),
            reservedSubmissionCount = NoticeSubmissionCount(reserved),
            recordedSentSubmissionCount = NoticeSubmissionCount(sent),
            lastOperatorLabel = OperatorLabel("operator"),
        )
    }
}
