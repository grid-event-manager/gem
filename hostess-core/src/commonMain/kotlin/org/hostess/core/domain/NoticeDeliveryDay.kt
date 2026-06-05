package org.hostess.core.domain

@JvmInline
value class NoticeDeliveryDay(val value: String) {
    init {
        require(DAY_PATTERN.matches(value)) { "NoticeDeliveryDay must be YYYY-MM-DD." }
    }

    companion object {
        private val DAY_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
