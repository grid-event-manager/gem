package org.hostess.core.domain

data class NoticeCompliancePolicy(
    val platformPerGroupDailyLimit: NoticeSubmissionCount = NoticeSubmissionCount(200),
    val hostessPerGroupDailyHardCap: NoticeSubmissionCount = NoticeSubmissionCount(180),
) {
    init {
        require(hostessPerGroupDailyHardCap.value <= platformPerGroupDailyLimit.value) {
            "Hostess per-group hard cap cannot exceed platform per-group limit."
        }
    }
}
