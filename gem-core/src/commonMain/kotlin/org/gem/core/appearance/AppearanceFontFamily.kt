package org.gem.core.appearance

class AppearanceFontFamily private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AppearanceFontFamily && value == other.value

    override fun hashCode(): Int =
        value.hashCode()

    override fun toString(): String =
        "AppearanceFontFamily(value=$value)"

    companion object {
        operator fun invoke(value: String): AppearanceFontFamily {
            val trimmed = value.trim()
            require(trimmed.isNotBlank()) { "AppearanceFontFamily cannot be blank." }
            return AppearanceFontFamily(trimmed)
        }
    }
}
