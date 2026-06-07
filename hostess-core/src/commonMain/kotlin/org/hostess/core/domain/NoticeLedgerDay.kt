package org.hostess.core.domain

@JvmInline
value class NoticeLedgerDay(val value: String) {
    init {
        require(DAY_PATTERN.matches(value)) { "NoticeLedgerDay must be YYYY-MM-DD." }
    }

    companion object {
        private val DAY_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
