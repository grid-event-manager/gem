package org.hostess.core.services

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceReceipt
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeLedgerDay
import org.hostess.core.domain.NoticeSubmissionCount
import org.hostess.core.domain.NoticeSubmissionLedgerSnapshot
import org.hostess.core.domain.NoticeSubmissionProjection
import org.hostess.core.ports.NoticeSubmissionLedgerPort

class NoticeComplianceService(
    private val policy: NoticeCompliancePolicy,
    private val ledger: NoticeSubmissionLedgerPort,
    private val clock: NoticeComplianceClock,
) {
    fun preflight(
        session: HostessSession,
        draft: NoticeDraft,
        request: NoticeComplianceRequest,
    ): NoticeComplianceDecision {
        val noticeLedgerDay = clock.currentSecondLifeDay()
        val projection = NoticeSubmissionProjection.from(draft.targetSet.selectedGroups)
        val snapshots = when (
            val result = ledger.snapshot(
                proofAccountLabel = session.accountLabel,
                operatorLabel = request.operatorLabel,
                groups = projection.selectedGroups,
                noticeLedgerDay = noticeLedgerDay,
            )
        ) {
            is NoticeComplianceLedgerResult.Success -> result.value
            is NoticeComplianceLedgerResult.Failure -> return denied(
                session = session,
                request = request,
                noticeLedgerDay = noticeLedgerDay,
                projection = projection,
                reasonCode = "ledger_snapshot_unavailable",
                ledgerMaxGroupTotal = NoticeSubmissionCount.ZERO,
            )
        }

        val projectedGroupTotals = snapshots.map { snapshot ->
            snapshot.reservedSubmissionCount
                .plus(snapshot.recordedSentSubmissionCount)
                .plus(NoticeSubmissionCount.ONE)
        }
        val ledgerMaxGroupTotal = projectedGroupTotals.maxByOrNull { it.value } ?: NoticeSubmissionCount.ZERO
        if (ledgerMaxGroupTotal.value > policy.hostessPerGroupDailyHardCap.value) {
            return denied(
                session = session,
                request = request,
                noticeLedgerDay = noticeLedgerDay,
                projection = projection,
                reasonCode = "notice_submission_cap_exceeded",
                ledgerMaxGroupTotal = ledgerMaxGroupTotal,
            )
        }

        return when (
            ledger.reserve(
                proofAccountLabel = session.accountLabel,
                operatorLabel = request.operatorLabel,
                projection = projection,
                noticeLedgerDay = noticeLedgerDay,
            )
        ) {
            is NoticeComplianceLedgerResult.Success -> NoticeComplianceDecision.Allowed(
                projection = projection,
                receipt = receipt(
                    session = session,
                    request = request,
                    noticeLedgerDay = noticeLedgerDay,
                    projection = projection,
                    ledgerMaxGroupTotal = ledgerMaxGroupTotal,
                    reasonCode = "allowed",
                ),
            )
            is NoticeComplianceLedgerResult.Failure -> denied(
                session = session,
                request = request,
                noticeLedgerDay = noticeLedgerDay,
                projection = projection,
                reasonCode = "ledger_reserve_failed",
                ledgerMaxGroupTotal = ledgerMaxGroupTotal,
            )
        }
    }

    fun recordSendResult(
        allowed: NoticeComplianceDecision.Allowed,
        sentGroups: List<GroupMembership>,
    ): NoticeComplianceReceipt =
        when (
            ledger.recordSendResult(
                proofAccountLabel = allowed.receipt.proofAccountLabel,
                operatorLabel = allowed.receipt.operatorLabel,
                projection = allowed.projection,
                sentGroups = sentGroups,
                noticeLedgerDay = allowed.receipt.noticeLedgerDay,
            )
        ) {
            is NoticeComplianceLedgerResult.Success -> allowed.receipt
            is NoticeComplianceLedgerResult.Failure -> allowed.receipt.copy(reasonCode = "ledger_record_failed")
        }

    private fun denied(
        session: HostessSession,
        request: NoticeComplianceRequest,
        noticeLedgerDay: NoticeLedgerDay,
        projection: NoticeSubmissionProjection,
        reasonCode: String,
        ledgerMaxGroupTotal: NoticeSubmissionCount,
    ): NoticeComplianceDecision.Denied =
        NoticeComplianceDecision.Denied(
            receipt(
                session = session,
                request = request,
                noticeLedgerDay = noticeLedgerDay,
                projection = projection,
                ledgerMaxGroupTotal = ledgerMaxGroupTotal,
                reasonCode = reasonCode,
            ),
        )

    private fun receipt(
        session: HostessSession,
        request: NoticeComplianceRequest,
        noticeLedgerDay: NoticeLedgerDay,
        projection: NoticeSubmissionProjection,
        ledgerMaxGroupTotal: NoticeSubmissionCount,
        reasonCode: String,
    ): NoticeComplianceReceipt =
        NoticeComplianceReceipt(
            operatorLabel = request.operatorLabel,
            proofAccountLabel = session.accountLabel,
            noticeLedgerDay = noticeLedgerDay,
            projectedSubmissionCount = projection.projectedSubmissionCount,
            ledgerGroupCount = projection.selectedGroups.size,
            ledgerMaxGroupTotal = ledgerMaxGroupTotal,
            hardCap = policy.hostessPerGroupDailyHardCap,
            reasonCode = reasonCode,
            redactedSourceSummary = "derived-from-selected-groups",
        )
}
