package org.hostess.core.domain

@JvmInline
value class NoticeRecipientCount(val value: Long) {
    init {
        require(value >= 0) { "NoticeRecipientCount cannot be negative." }
    }
}
