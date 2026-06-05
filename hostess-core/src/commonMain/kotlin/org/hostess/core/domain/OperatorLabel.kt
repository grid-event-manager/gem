package org.hostess.core.domain

@JvmInline
value class OperatorLabel(val value: String) {
    init {
        require(value.isNotBlank()) { "OperatorLabel cannot be blank." }
        require(value == value.trim()) { "OperatorLabel must be trimmed." }
    }
}
