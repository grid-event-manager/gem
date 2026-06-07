package org.hostess.core.services

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.HostessSession
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
) {
    fun dispatch(
        session: HostessSession,
        draft: NoticeDraft,
        pacingPolicy: PacingPolicy = PacingPolicy.NONE,
        attachment: AttachmentRef? = null,
    ): NoticeDispatchResult {
        val validation = draft.validateForSend()
        if (validation != NoticeDraftValidation.Valid) {
            return NoticeDispatchResult.Rejected(validation)
        }

        val plan = NoticeSendPlan(draft, pacingPolicy)
        val statuses = plan.targetGroups.mapIndexed { index, group ->
            if (index > 0) {
                applyPacing(pacingPolicy)
            }
            noticePort.sendGroupNotice(session, group, draft, attachment)
        }
        return NoticeDispatchResult.Sent(NoticeSendResult(plan, statuses))
    }

    private fun applyPacing(pacingPolicy: PacingPolicy) {
        if (pacingPolicy.delayBetweenGroups.isPositive) {
            clockPort.pause(pacingPolicy.delayBetweenGroups)
        }
    }
}
