package org.gem.core.appearance

class AppearanceProfileName private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AppearanceProfileName && value == other.value

    override fun hashCode(): Int =
        value.hashCode()

    override fun toString(): String =
        "AppearanceProfileName(value=$value)"

    companion object {
        operator fun invoke(value: String): AppearanceProfileName {
            val trimmed = value.trim()
            require(trimmed.isNotBlank()) { "AppearanceProfileName cannot be blank." }
            return AppearanceProfileName(trimmed)
        }
    }
}
