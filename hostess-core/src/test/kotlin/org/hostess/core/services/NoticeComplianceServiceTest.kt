package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeRecipientCount
import org.hostess.core.domain.NoticeRecipientEstimate
import org.hostess.core.domain.NoticeRecipientEstimateSource
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.NoticeComplianceLedgerPort

class NoticeComplianceServiceTest {
    @Test
    fun `allows below cap and exactly at hard cap while reserving projected deliveries`() {
        val belowLedger = RecordingNoticeComplianceLedger(
            snapshotResult = NoticeComplianceLedgerResult.Success(snapshot(reserved = 1_000, sent = 500)),
        )
        val below = assertIs<NoticeComplianceDecision.Allowed>(
            service(belowLedger).preflight(session(), draft(), request(music = 125, gallery = 75)),
        )

        assertEquals(NoticeDeliveryCount(200), below.projection.projectedDeliveryCount)
        assertEquals(NoticeDeliveryCount(1_500), below.receipt.previousLedgerCount)
        assertEquals(NoticeDeliveryCount(1_700), below.receipt.projectedLedgerTotal)
        assertEquals(listOf(NoticeDeliveryCount(200)), belowLedger.reserveCalls.map { it.projected })

        val exactLedger = RecordingNoticeComplianceLedger(
            snapshotResult = NoticeComplianceLedgerResult.Success(snapshot(reserved = 4_400, sent = 0)),
        )
        val exact = assertIs<NoticeComplianceDecision.Allowed>(
            service(exactLedger).preflight(session(), draft(), request(music = 60, gallery = 40)),
        )

        assertEquals(NoticeDeliveryCount(4_500), exact.receipt.projectedLedgerTotal)
        assertEquals("allowed", exact.receipt.reasonCode)
        assertEquals(listOf(NoticeDeliveryCount(100)), exactLedger.reserveCalls.map { it.projected })
    }

    @Test
    fun `denies cap breach unknown duplicate snapshot failure and reserve failure`() {
        val capExceeded = service(
            RecordingNoticeComplianceLedger(
                snapshotResult = NoticeComplianceLedgerResult.Success(snapshot(reserved = 4_450, sent = 0)),
            ),
        ).preflight(session(), draft(), request(music = 26, gallery = 25))
        assertDenied("recipient_delivery_cap_exceeded", capExceeded)

        val unknown = service(RecordingNoticeComplianceLedger()).preflight(
            session(),
            draft(),
            request(
                estimate("Music Room", 10, NoticeRecipientEstimateSource.UNKNOWN),
                estimate("Gallery", 10),
            ),
        )
        assertDenied("recipient_count_unknown", unknown)

        val duplicate = service(RecordingNoticeComplianceLedger()).preflight(
            session(),
            draft(),
            request(
                estimate("Music Room", 10),
                estimate("Music Room", 11),
                estimate("Gallery", 10),
            ),
        )
        assertDenied("recipient_count_ambiguous", duplicate)

        val missing = service(RecordingNoticeComplianceLedger()).preflight(
            session(),
            draft(),
            request(estimate("Music Room", 10)),
        )
        assertDenied("recipient_count_unknown", missing)

        val snapshotFailure = service(
            RecordingNoticeComplianceLedger(
                snapshotResult = NoticeComplianceLedgerResult.Failure("disk unavailable"),
            ),
        ).preflight(session(), draft(), request())
        assertDenied("ledger_snapshot_unavailable", snapshotFailure)

        val reserveFailure = service(
            RecordingNoticeComplianceLedger(
                reserveResult = NoticeComplianceLedgerResult.Failure("reserve unavailable"),
            ),
        ).preflight(session(), draft(), request())
        assertDenied("ledger_reserve_failed", reserveFailure)
    }

    @Test
    fun `default clock maps UTC instants to Second Life delivery day`() {
        val beforeMidnight = DefaultNoticeComplianceClock { "2026-06-04" }
        val afterMidnight = DefaultNoticeComplianceClock { "2026-06-05" }

        assertEquals(NoticeDeliveryDay("2026-06-04"), beforeMidnight.currentSecondLifeDay())
        assertEquals(NoticeDeliveryDay("2026-06-05"), afterMidnight.currentSecondLifeDay())
    }

    @Test
    fun `record send result returns failed receipt when ledger record fails`() {
        val ledger = RecordingNoticeComplianceLedger(
            recordResult = NoticeComplianceLedgerResult.Failure("record unavailable"),
        )
        val allowed = assertIs<NoticeComplianceDecision.Allowed>(
            service(ledger).preflight(session(), draft(), request(music = 20, gallery = 30)),
        )

        val receipt = service(ledger).recordSendResult(allowed, NoticeDeliveryCount(20))

        assertEquals("ledger_record_failed", receipt.reasonCode)
        assertEquals(
            listOf(RecordCall(NoticeDeliveryCount(50), NoticeDeliveryCount(20))),
            ledger.recordCalls,
        )
    }

    private fun service(
        ledger: NoticeComplianceLedgerPort,
        clock: NoticeComplianceClock = NoticeComplianceClock { NoticeDeliveryDay("2026-06-05") },
    ): NoticeComplianceService = NoticeComplianceService(NoticeCompliancePolicy(), ledger, clock)

    private fun request(
        vararg estimates: NoticeRecipientEstimate,
    ): NoticeComplianceRequest = NoticeComplianceRequest(
        operatorLabel = OperatorLabel("operator"),
        recipientEstimates = estimates.toList().ifEmpty {
            listOf(estimate("Music Room", 100), estimate("Gallery", 100))
        },
    )

    private fun request(
        music: Long,
        gallery: Long,
    ): NoticeComplianceRequest = request(
        estimate("Music Room", music),
        estimate("Gallery", gallery),
    )

    private fun estimate(
        displayName: String,
        count: Long,
        source: NoticeRecipientEstimateSource = NoticeRecipientEstimateSource.OPERATOR_ACKNOWLEDGED,
    ): NoticeRecipientEstimate = NoticeRecipientEstimate(
        displayName = GroupDisplayName(displayName),
        recipientCount = NoticeRecipientCount(count),
        source = source,
    )

    private fun draft(): NoticeDraft = NoticeDraft(
        subject = "Tonight",
        message = "Doors at eight",
        targetSet = selectedTargets(),
    )

    private fun selectedTargets(): GroupTargetSet = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(
            listOf(
                GroupMembership.fromValues("music", "Music Room", true, true),
                GroupMembership.fromValues("gallery", "Gallery", true, true),
            ),
        ).addAllSendable(),
    ).targetSet

    private fun session(): HostessSession = HostessSession(
        sessionId = SessionId("session"),
        accountLabel = AccountLabel("proof-account"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private fun snapshot(
        reserved: Long = 0,
        sent: Long = 0,
    ): NoticeDeliveryLedgerSnapshot = NoticeDeliveryLedgerSnapshot(
        operatorLabel = OperatorLabel("operator"),
        deliveryDay = NoticeDeliveryDay("2026-06-05"),
        reservedDeliveryCount = NoticeDeliveryCount(reserved),
        recordedSentDeliveryCount = NoticeDeliveryCount(sent),
    )

    private fun assertDenied(
        reasonCode: String,
        decision: NoticeComplianceDecision,
    ) {
        val denied = assertIs<NoticeComplianceDecision.Denied>(decision)
        assertEquals(reasonCode, denied.receipt.reasonCode)
    }

    private data class ReserveCall(val projected: NoticeDeliveryCount)

    private data class RecordCall(
        val reservedProjection: NoticeDeliveryCount,
        val delivered: NoticeDeliveryCount,
    )

    private class RecordingNoticeComplianceLedger(
        private val snapshotResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> =
            NoticeComplianceLedgerResult.Success(emptySnapshot()),
        private val reserveResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>? = null,
        private val recordResult: NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>? = null,
    ) : NoticeComplianceLedgerPort {
        val reserveCalls = mutableListOf<ReserveCall>()
        val recordCalls = mutableListOf<RecordCall>()

        override fun snapshot(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> = snapshotResult

        override fun reserve(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            projected: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
            reserveCalls += ReserveCall(projected)
            return reserveResult ?: NoticeComplianceLedgerResult.Success(
                emptySnapshot(reserved = projected.value),
            )
        }

        override fun recordSendResult(
            operatorLabel: OperatorLabel,
            deliveryDay: NoticeDeliveryDay,
            reservedProjection: NoticeDeliveryCount,
            delivered: NoticeDeliveryCount,
        ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot> {
            recordCalls += RecordCall(reservedProjection, delivered)
            return recordResult ?: NoticeComplianceLedgerResult.Success(
                emptySnapshot(sent = delivered.value),
            )
        }

        private companion object {
            fun emptySnapshot(
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
}
