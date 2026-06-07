package org.hostess.core.domain

class NoticeSubmissionProjection private constructor(
    val selectedGroups: List<GroupMembership>,
) {
    val projectedSubmissionCount: NoticeSubmissionCount = NoticeSubmissionCount(selectedGroups.size.toLong())

    init {
        require(selectedGroups.isNotEmpty()) { "NoticeSubmissionProjection requires selected groups." }
    }

    companion object {
        fun from(selectedGroups: List<GroupMembership>): NoticeSubmissionProjection =
            NoticeSubmissionProjection(selectedGroups = selectedGroups.toList())
    }
}
