package org.hostess.core.services

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeComplianceDecision
import org.hostess.core.domain.NoticeComplianceRequest
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDispatchResult
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.NoticeSendPlan
import org.hostess.core.domain.NoticeSendResult
import org.hostess.core.domain.PacingPolicy
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.NoticePort

class NoticeDispatchService(
    private val noticePort: NoticePort,
    private val clockPort: ClockPort,
    private val noticeComplianceService: NoticeComplianceService,
) {
    fun dispatch(
        session: HostessSession,
        draft: NoticeDraft,
        compliance: NoticeComplianceRequest,
        pacingPolicy: PacingPolicy = PacingPolicy.NONE,
        attachment: AttachmentRef? = null,
    ): NoticeDispatchResult {
        val validation = draft.validateForSend()
        if (validation != NoticeDraftValidation.Valid) {
            return NoticeDispatchResult.Rejected(validation)
        }

        val complianceDecision = noticeComplianceService.preflight(session, draft, compliance)
        if (complianceDecision is NoticeComplianceDecision.Denied) {
            return NoticeDispatchResult.ComplianceRejected(complianceDecision)
        }
        complianceDecision as NoticeComplianceDecision.Allowed

        val plan = NoticeSendPlan(draft, pacingPolicy)
        val statuses = plan.targetGroups.mapIndexed { index, group ->
            if (index > 0) {
                applyPacing(pacingPolicy)
            }
            noticePort.sendGroupNotice(session, group, draft, attachment)
        }
        val sendResult = NoticeSendResult(plan, statuses)
        val receipt = noticeComplianceService.recordSendResult(
            allowed = complianceDecision,
            delivered = deliveredCount(complianceDecision, sendResult),
        )

        return if (receipt.reasonCode == "ledger_record_failed") {
            NoticeDispatchResult.ComplianceRecordFailed(sendResult, receipt)
        } else {
            NoticeDispatchResult.Sent(sendResult, receipt)
        }
    }

    private fun applyPacing(pacingPolicy: PacingPolicy) {
        if (pacingPolicy.delayBetweenGroups.isPositive) {
            clockPort.pause(pacingPolicy.delayBetweenGroups)
        }
    }

    private fun deliveredCount(
        allowed: NoticeComplianceDecision.Allowed,
        sendResult: NoticeSendResult,
    ): NoticeDeliveryCount {
        val estimatesByGroupId = allowed.projection.selectedGroups
            .zip(allowed.projection.estimates)
            .associate { (group, estimate) ->
                group.groupId to estimate.recipientCount.value
            }

        return sendResult.statuses.fold(NoticeDeliveryCount.ZERO) { total, status ->
            if (status.state != GroupSendState.SENT) {
                total
            } else {
                total.plus(NoticeDeliveryCount(estimatesByGroupId.getValue(status.group.groupId)))
            }
        }
    }
}
