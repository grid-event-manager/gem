package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeCompliancePolicy
import org.hostess.core.domain.NoticeComplianceReceipt
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.NoticeDeliveryProjection
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeRecipientEstimate
import org.hostess.core.domain.NoticeRecipientEstimateSource
import org.hostess.core.ports.NoticeComplianceLedgerPort

class NoticeComplianceService(
    private val policy: NoticeCompliancePolicy,
    private val ledger: NoticeComplianceLedgerPort,
    private val clock: NoticeComplianceClock,
) {
    fun preflight(
        session: HostessSession,
        draft: NoticeDraft,
        request: NoticeComplianceRequest,
    ): NoticeComplianceDecision {
        val deliveryDay = clock.currentSecondLifeDay()
        val selectedGroups = draft.targetSet.selectedGroups
        val selectedNames = selectedGroups.map { it.displayName }
        val estimatesByName = request.recipientEstimates.groupBy { it.displayName }

        val missingEstimate = selectedNames.firstOrNull { estimatesByName[it].isNullOrEmpty() }
        if (missingEstimate != null) {
            return denied(request, deliveryDay, "recipient_count_unknown")
        }

        val duplicateEstimate = selectedNames.firstOrNull { estimatesByName.getValue(it).size > 1 }
        if (duplicateEstimate != null) {
            return denied(request, deliveryDay, "recipient_count_ambiguous")
        }

        val selectedEstimates = selectedNames.map { estimatesByName.getValue(it).single() }
        if (selectedEstimates.any { it.source == NoticeRecipientEstimateSource.UNKNOWN }) {
            return denied(request, deliveryDay, "recipient_count_unknown")
        }

        val projection = NoticeDeliveryProjection(
            selectedGroups = selectedGroups,
            projectedDeliveryCount = selectedEstimates.projectedDeliveryCount(),
            estimates = selectedEstimates,
        )
        val snapshot = when (val result = ledger.snapshot(request.operatorLabel, deliveryDay)) {
            is NoticeComplianceLedgerResult.Success -> result.value
            is NoticeComplianceLedgerResult.Failure -> return denied(
                request = request,
                deliveryDay = deliveryDay,
                reasonCode = "ledger_snapshot_unavailable",
                projection = projection,
            )
        }
        val previousLedgerCount = snapshot.previousLedgerCount()
        val projectedLedgerTotal = previousLedgerCount.plus(projection.projectedDeliveryCount)
        if (projectedLedgerTotal.value > policy.hostessHardCap.value) {
            return NoticeComplianceDecision.Denied(
                receipt(
                    request = request,
                    deliveryDay = deliveryDay,
                    projection = projection,
                    previousLedgerCount = previousLedgerCount,
                    projectedLedgerTotal = projectedLedgerTotal,
                    reasonCode = "recipient_delivery_cap_exceeded",
                ),
            )
        }

        return when (ledger.reserve(request.operatorLabel, deliveryDay, projection.projectedDeliveryCount)) {
            is NoticeComplianceLedgerResult.Success -> NoticeComplianceDecision.Allowed(
                projection = projection,
                receipt = receipt(
                    request = request,
                    deliveryDay = deliveryDay,
                    projection = projection,
                    previousLedgerCount = previousLedgerCount,
                    projectedLedgerTotal = projectedLedgerTotal,
                    reasonCode = "allowed",
                ),
            )
            is NoticeComplianceLedgerResult.Failure -> NoticeComplianceDecision.Denied(
                receipt(
                    request = request,
                    deliveryDay = deliveryDay,
                    projection = projection,
                    previousLedgerCount = previousLedgerCount,
                    projectedLedgerTotal = projectedLedgerTotal,
                    reasonCode = "ledger_reserve_failed",
                ),
            )
        }
    }

    fun recordSendResult(
        allowed: NoticeComplianceDecision.Allowed,
        delivered: NoticeDeliveryCount,
    ): NoticeComplianceReceipt =
        when (
            ledger.recordSendResult(
                allowed.receipt.operatorLabel,
                allowed.receipt.deliveryDay,
                allowed.projection.projectedDeliveryCount,
                delivered,
            )
        ) {
            is NoticeComplianceLedgerResult.Success -> allowed.receipt
            is NoticeComplianceLedgerResult.Failure -> allowed.receipt.copy(reasonCode = "ledger_record_failed")
        }

    private fun List<NoticeRecipientEstimate>.projectedDeliveryCount(): NoticeDeliveryCount =
        fold(NoticeDeliveryCount.ZERO) { total, estimate ->
            total.plus(NoticeDeliveryCount(estimate.recipientCount.value))
        }

    private fun NoticeDeliveryLedgerSnapshot.previousLedgerCount(): NoticeDeliveryCount =
        reservedDeliveryCount.plus(recordedSentDeliveryCount)

    private fun denied(
        request: NoticeComplianceRequest,
        deliveryDay: NoticeDeliveryDay,
        reasonCode: String,
        projection: NoticeDeliveryProjection? = null,
    ): NoticeComplianceDecision.Denied =
        NoticeComplianceDecision.Denied(
            receipt(
                request = request,
                deliveryDay = deliveryDay,
                projection = projection,
                previousLedgerCount = NoticeDeliveryCount.ZERO,
                projectedLedgerTotal = projection?.projectedDeliveryCount ?: NoticeDeliveryCount.ZERO,
                reasonCode = reasonCode,
            ),
        )

    private fun receipt(
        request: NoticeComplianceRequest,
        deliveryDay: NoticeDeliveryDay,
        projection: NoticeDeliveryProjection?,
        previousLedgerCount: NoticeDeliveryCount,
        projectedLedgerTotal: NoticeDeliveryCount,
        reasonCode: String,
    ): NoticeComplianceReceipt =
        NoticeComplianceReceipt(
            operatorLabel = request.operatorLabel,
            deliveryDay = deliveryDay,
            projectedDeliveryCount = projection?.projectedDeliveryCount ?: NoticeDeliveryCount.ZERO,
            previousLedgerCount = previousLedgerCount,
            projectedLedgerTotal = projectedLedgerTotal,
            hardCap = policy.hostessHardCap,
            reasonCode = reasonCode,
            redactedSourceSummary = sourceSummary(projection?.estimates.orEmpty()),
        )

    private fun sourceSummary(estimates: List<NoticeRecipientEstimate>): String {
        val sources = estimates.mapTo(sortedSetOf()) { it.source.name.lowercase() }
        return "selected=${estimates.size};sources=${sources.joinToString("|")}"
    }
}
