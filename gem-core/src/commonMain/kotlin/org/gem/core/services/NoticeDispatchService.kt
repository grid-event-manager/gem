package org.gem.core.services

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDispatchResult
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.NoticeSendPlan
import org.gem.core.domain.NoticeSendResult
import org.gem.core.domain.PacingPolicy
import org.gem.core.ports.ClockPort
import org.gem.core.ports.NoticePort

class NoticeDispatchService(
    private val noticePort: NoticePort,
    private val clockPort: ClockPort,
) {
    fun dispatch(
        session: GemSession,
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
