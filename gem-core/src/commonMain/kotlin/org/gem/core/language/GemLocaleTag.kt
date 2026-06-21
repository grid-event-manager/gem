package org.gem.core.language

data class GemLocaleTag(val value: String) {
    init {
        require(value.isNotBlank()) { "localeTag must not be blank" }
    }

    override fun toString(): String =
        value
}
