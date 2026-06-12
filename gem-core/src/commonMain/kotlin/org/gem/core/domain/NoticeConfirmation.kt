package org.gem.core.domain

class NoticeConfirmationResult(
    statuses: List<GroupNoticeConfirmationStatus>,
) {
    val statuses: List<GroupNoticeConfirmationStatus> = statuses.toList()

    val allConfirmed: Boolean
        get() = statuses.isNotEmpty() && statuses.all { it.state == GroupNoticeConfirmationState.CONFIRMED }
}

data class GroupNoticeConfirmationStatus(
    val group: GroupMembership,
    val state: GroupNoticeConfirmationState,
    val detail: String? = null,
)

enum class GroupNoticeConfirmationState {
    CONFIRMED,
    UNCONFIRMED,
    SKIPPED,
    FAILED,
}
