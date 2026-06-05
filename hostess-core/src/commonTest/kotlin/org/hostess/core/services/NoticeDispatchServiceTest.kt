package org.hostess.core.services

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.NoticeRecipientCount
import org.hostess.core.domain.NoticeRecipientEstimate
import org.hostess.core.domain.NoticeRecipientEstimateSource
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.NoticeComplianceLedgerPort
import org.hostess.core.testing.FakeClockPort
import org.hostess.core.testing.FakeNoticePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeDispatchServiceTest {
    @Test
    fun `dispatch calls notice port once per selected group and applies pacing between groups`() {
        val events = mutableListOf<String>()
        val noticePort = FakeNoticePort(
            events = events,
            statesByGroup = mapOf(
                GroupId("music") to GroupSendState.SENT,
                GroupId("gallery") to GroupSendState.FAILED,
            ),
        )
        val service = service(noticePort, FakeClockPort(events))
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
    }

    @Test
    fun `dispatch rejects invalid draft without calling notice port`() {
        val events = mutableListOf<String>()
        val ledger = RecordingNoticeComplianceLedger()
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
            ledger = RecordingNoticeComplianceLedger(
                snapshotResult = NoticeComplianceLedgerResult.Success(snapshot(reserved = 4_499)),
            ),
        )
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
        )

        val result = assertIs<NoticeDispatchResult.ComplianceRejected>(
            service.dispatch(session(), draft, request(music = 2, gallery = 0)),
        )

        assertIs<NoticeComplianceDecision.Denied>(result.decision)
        assertEquals("recipient_delivery_cap_exceeded", result.decision.receipt.reasonCode)
        assertTrue(events.isEmpty())
        assertTrue(noticePort.calls.isEmpty())
    }

    @Test
    fun `dispatch surfaces ledger record failure after sent statuses`() {
        val events = mutableListOf<String>()
        val ledger = RecordingNoticeComplianceLedger(
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
            service.dispatch(session(), draft, request(music = 10, gallery = 20)),
        )

        assertEquals("ledger_record_failed", result.complianceReceipt.reasonCode)
        assertEquals(listOf(GroupSendState.SENT, GroupSendState.SENT), result.result.statuses.map { it.state })
        assertEquals(listOf(RecordCall(NoticeDeliveryCount(30), NoticeDeliveryCount(30))), ledger.recordCalls)
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
        ledger: NoticeComplianceLedgerPort = RecordingNoticeComplianceLedger(),
    ): NoticeDispatchService = NoticeDispatchService(
        noticePort = noticePort,
        clockPort = clockPort,
        noticeComplianceService = NoticeComplianceService(
            policy = NoticeCompliancePolicy(),
            ledger = ledger,
            clock = NoticeComplianceClock { NoticeDeliveryDay("2026-06-05") },
        ),
    )

    private fun request(
        music: Long = 100,
        gallery: Long = 100,
    ): NoticeComplianceRequest = NoticeComplianceRequest(
        operatorLabel = OperatorLabel("operator"),
        recipientEstimates = listOf(
            estimate("Music Room", music),
            estimate("Gallery", gallery),
        ),
    )

    private fun estimate(
        displayName: String,
        count: Long,
    ): NoticeRecipientEstimate = NoticeRecipientEstimate(
        displayName = GroupDisplayName(displayName),
        recipientCount = NoticeRecipientCount(count),
        source = NoticeRecipientEstimateSource.OPERATOR_ACKNOWLEDGED,
    )

    private data class RecordCall(
        val reservedProjection: NoticeDeliveryCount,
        val delivered: NoticeDeliveryCount,
    )

    private class RecordingNoticeComplianceLedger(
        private val snapshotResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
            NoticeComplianceLedgerResult.Success(snapshot()),
        private val reserveResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>? = null,
        private val recordResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>? = null,
    ) : NoticeComplianceLedgerPort {
        var snapshotCalls = 0
        val reserveCalls = mutableListOf<NoticeDeliveryCount>()
        val recordCalls = mutableListOf<RecordCall>()

        override fun snapshot(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
            snapshotCalls += 1
            return snapshotResult
        }

        override fun reserve(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            projected: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
            reserveCalls += projected
            return reserveResult ?: NoticeComplianceLedgerResult.Success(snapshot(reserved = projected.value))
        }

        override fun recordSendResult(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            reservedProjection: NoticeDeliveryCount,
            delivered: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
            recordCalls += RecordCall(reservedProjection, delivered)
            return recordResult ?: NoticeComplianceLedgerResult.Success(snapshot(sent = delivered.value))
        }
    }

    private companion object {
        fun snapshot(
            reserved: Long = 0,
            sent: Long = 0,
        ): NoticeDeliveryLedgerSnapshot = NoticeDeliveryLedgerSnapshot(
            operatorLabel = OperatorLabel("operator"),
            deliveryDay = NoticeDeliveryDay("2026-06-05"),
            reservedDeliveryCount = NoticeDeliveryCount(reserved),
            recordedSentDeliveryCount = NoticeDeliveryCount(sent),
        )
    }
}
