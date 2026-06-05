package org.hostess.core.domain

@JvmInline
value class NoticeDeliveryCount(val value: Long) {
    init {
        require(value >= 0) { "NoticeDeliveryCount cannot be negative." }
    }

    fun plus(other: NoticeDeliveryCount): NoticeDeliveryCount =
        NoticeDeliveryCount(Math.addExact(value, other.value))

    companion object {
        val ZERO: NoticeDeliveryCount = NoticeDeliveryCount(0)
    }
}
