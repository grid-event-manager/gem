package org.hostess.core.domain

@JvmInline
value class NoticeSubmissionCount(val value: Long) {
    init {
        require(value >= 0) { "NoticeSubmissionCount cannot be negative." }
    }

    fun plus(other: NoticeSubmissionCount): NoticeSubmissionCount =
        NoticeSubmissionCount(Math.addExact(value, other.value))

    companion object {
        val ZERO: NoticeSubmissionCount = NoticeSubmissionCount(0)
        val ONE: NoticeSubmissionCount = NoticeSubmissionCount(1)
    }
}
