package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionCount
import org.hostess.core.domain.NoticeSubmissionLedgerSnapshot
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.NoticeSubmissionLedgerPort

class NoticeComplianceServiceTest {
    @Test
    fun `allows below cap and exactly at hard cap while reserving projected submissions`() {
        val belowLedger = RecordingNoticeSubmissionLedger(
            snapshotResult = NoticeComplianceLedgerResult.Success(
                listOf(snapshot("music", "Music Room", reserved = 100, sent = 50), snapshot("gallery", "Gallery")),
            ),
        )
        val below = assertIs<NoticeComplianceDecision.Allowed>(
            service(belowLedger).preflight(session(), draft(), request()),
        )

        assertEquals(NoticeSubmissionCount(2), below.projection.projectedSubmissionCount)
        assertEquals(2, below.receipt.ledgerGroupCount)
        assertEquals(NoticeSubmissionCount(151), below.receipt.ledgerMaxGroupTotal)
        assertEquals(listOf(listOf("music", "gallery")), belowLedger.reserveCalls.map { it.groupIds })

        val exactLedger = RecordingNoticeSubmissionLedger(
            snapshotResult = NoticeComplianceLedgerResult.Success(
                listOf(snapshot("music", "Music Room", reserved = 179), snapshot("gallery", "Gallery")),
            ),
        )
        val exact = assertIs<NoticeComplianceDecision.Allowed>(
            service(exactLedger).preflight(session(), draft(), request()),
        )

        assertEquals(NoticeSubmissionCount(180), exact.receipt.ledgerMaxGroupTotal)
        assertEquals("allowed", exact.receipt.reasonCode)
        assertEquals(listOf(listOf("music", "gallery")), exactLedger.reserveCalls.map { it.groupIds })
    }

    @Test
    fun `denies cap breach snapshot failure and reserve failure`() {
        val capExceeded = service(
            RecordingNoticeSubmissionLedger(
                snapshotResult = NoticeComplianceLedgerResult.Success(
                    listOf(snapshot("music", "Music Room", reserved = 180), snapshot("gallery", "Gallery")),
                ),
            ),
        ).preflight(session(), draft(), request())
        val capReceipt = assertDenied("notice_submission_cap_exceeded", capExceeded)
        assertEquals(NoticeSubmissionCount(181), capReceipt.ledgerMaxGroupTotal)

        val snapshotFailure = service(
            RecordingNoticeSubmissionLedger(
                snapshotResult = NoticeComplianceLedgerResult.Failure("disk unavailable"),
            ),
        ).preflight(session(), draft(), request())
        val snapshotReceipt = assertDenied("ledger_snapshot_unavailable", snapshotFailure)
        assertEquals(2, snapshotReceipt.ledgerGroupCount)
        assertEquals(NoticeSubmissionCount.ZERO, snapshotReceipt.ledgerMaxGroupTotal)

        val reserveFailure = service(
            RecordingNoticeSubmissionLedger(
                reserveResult = NoticeComplianceLedgerResult.Failure("reserve unavailable"),
            ),
        ).preflight(session(), draft(), request())
        assertDenied("ledger_reserve_failed", reserveFailure)
    }

    @Test
    fun `record send result releases attempted projection and records sent groups only`() {
        val ledger = RecordingNoticeSubmissionLedger()
        val allowed = assertIs<NoticeComplianceDecision.Allowed>(
            service(ledger).preflight(session(), draft(), request()),
        )

        val receipt = service(ledger).recordSendResult(
            allowed = allowed,
            sentGroups = listOf(group("music", "Music Room")),
        )

        assertEquals("allowed", receipt.reasonCode)
        assertEquals(
            listOf(RecordCall(projectionGroupIds = listOf("music", "gallery"), sentGroupIds = listOf("music"))),
            ledger.recordCalls,
        )
    }

    @Test
    fun `record send result returns failed receipt when ledger record fails`() {
        val ledger = RecordingNoticeSubmissionLedger(
            recordResult = NoticeComplianceLedgerResult.Failure("record unavailable"),
        )
        val allowed = assertIs<NoticeComplianceDecision.Allowed>(
            service(ledger).preflight(session(), draft(), request()),
        )

        val receipt = service(ledger).recordSendResult(allowed, listOf(group("music", "Music Room")))

        assertEquals("ledger_record_failed", receipt.reasonCode)
        assertEquals(
            listOf(RecordCall(projectionGroupIds = listOf("music", "gallery"), sentGroupIds = listOf("music"))),
            ledger.recordCalls,
        )
    }

    private fun service(
        ledger: NoticeSubmissionLedgerPort,
        clock: NoticeComplianceClock = NoticeComplianceClock { NoticeLedgerDay("2026-06-05") },
    ): NoticeComplianceService = NoticeComplianceService(NoticeCompliancePolicy(), ledger, clock)

    private fun request(): NoticeComplianceRequest = NoticeComplianceRequest(OperatorLabel("operator"))

    private fun draft(): NoticeDraft = NoticeDraft(
        subject = "Tonight",
        message = "Doors at eight",
        targetSet = selectedTargets(),
    )

    private fun selectedTargets(): GroupTargetSet = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(
            listOf(
                group("music", "Music Room"),
                group("gallery", "Gallery"),
            ),
        ).addAllSendable(),
    ).targetSet

    private fun group(id: String, displayName: String): GroupMembership = GroupMembership.fromValues(
        groupId = id,
        displayName = displayName,
        canSendNotices = true,
        acceptsNotices = true,
    )

    private fun session(): HostessSession = HostessSession(
        sessionId = SessionId("session"),
        accountLabel = AccountLabel("proof-account"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private fun snapshot(
        groupId: String,
        displayName: String,
        reserved: Long = 0,
        sent: Long = 0,
    ): NoticeSubmissionLedgerSnapshot = NoticeSubmissionLedgerSnapshot(
        proofAccountLabel = AccountLabel("proof-account"),
        groupId = GroupId(groupId),
        groupDisplayName = GroupDisplayName(displayName),
        noticeLedgerDay = NoticeLedgerDay("2026-06-05"),
        reservedSubmissionCount = NoticeSubmissionCount(reserved),
        recordedSentSubmissionCount = NoticeSubmissionCount(sent),
        lastOperatorLabel = OperatorLabel("operator"),
    )

    private fun assertDenied(
        reasonCode: String,
        decision: NoticeComplianceDecision,
    ) = assertIs<NoticeComplianceDecision.Denied>(decision).receipt.also {
        assertEquals(reasonCode, it.reasonCode)
    }

    private data class ReserveCall(val groupIds: List<String>)

    private data class RecordCall(
        val projectionGroupIds: List<String>,
        val sentGroupIds: List<String>,
    )

    private class RecordingNoticeSubmissionLedger(
        private val snapshotResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
        private val reserveResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
        private val recordResult: NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>>? = null,
    ) : NoticeSubmissionLedgerPort {
        val reserveCalls = mutableListOf<ReserveCall>()
        val recordCalls = mutableListOf<RecordCall>()

        override fun snapshot(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            groups: List<GroupMembership>,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> =
            snapshotResult ?: NoticeComplianceLedgerResult.Success(
                groups.map { group -> emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay) },
            )

        override fun reserve(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            projection: NoticeSubmissionProjection,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeComplianceLedgerResult<List<NoticeSubmissionLedgerSnapshot>> {
            reserveCalls += ReserveCall(projection.selectedGroups.map { it.groupId.value })
            return reserveResult ?: NoticeComplianceLedgerResult.Success(
                projection.selectedGroups.map { group -> emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay) },
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
                projection.selectedGroups.map { group -> emptySnapshot(proofAccountLabel, operatorLabel, group, noticeLedgerDay) },
            )
        }

        private fun emptySnapshot(
            proofAccountLabel: AccountLabel,
            operatorLabel: OperatorLabel,
            group: GroupMembership,
            noticeLedgerDay: NoticeLedgerDay,
        ): NoticeSubmissionLedgerSnapshot = NoticeSubmissionLedgerSnapshot(
            proofAccountLabel = proofAccountLabel,
            groupId = group.groupId,
            groupDisplayName = group.displayName,
            noticeLedgerDay = noticeLedgerDay,
            reservedSubmissionCount = NoticeSubmissionCount.ZERO,
            recordedSentSubmissionCount = NoticeSubmissionCount.ZERO,
            lastOperatorLabel = operatorLabel,
        )
    }
}
