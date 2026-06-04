package org.hostess.core.domain

import java.time.Duration

data class PacingPolicy(
    val delayBetweenGroups: Duration,
) {
    init {
        require(!delayBetweenGroups.isNegative) { "Pacing delay cannot be negative." }
    }

    companion object {
        val NONE: PacingPolicy = PacingPolicy(Duration.ZERO)
    }
}

data class NoticeSendPlan(
    val draft: NoticeDraft,
    val pacingPolicy: PacingPolicy = PacingPolicy.NONE,
) {
    init {
        require(draft.validateForSend() == NoticeDraftValidation.Valid) {
            "NoticeSendPlan requires a valid notice draft."
        }
    }

    val targetGroups: List<GroupMembership>
        get() = draft.targetSet.selectedGroups
}

sealed interface NoticeDispatchResult {
    data class Sent(val result: NoticeSendResult) : NoticeDispatchResult
    data class Rejected(val validation: NoticeDraftValidation) : NoticeDispatchResult
}

class NoticeSendResult(
    val plan: NoticeSendPlan,
    statuses: List<GroupSendStatus>,
) {
    val statuses: List<GroupSendStatus> = statuses.toList()

    init {
        require(this.statuses.isNotEmpty()) { "NoticeSendResult requires at least one group status." }
    }
}

data class GroupSendStatus(
    val group: GroupMembership,
    val state: GroupSendState,
    val detail: String? = null,
)

enum class GroupSendState {
    PENDING,
    SENT,
    SKIPPED,
    FAILED,
}
