package org.gem.core.appearance

class AppearanceProfileId private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AppearanceProfileId && value == other.value

    override fun hashCode(): Int =
        value.hashCode()

    override fun toString(): String =
        "AppearanceProfileId(value=$value)"

    companion object {
        operator fun invoke(value: String): AppearanceProfileId {
            val trimmed = value.trim()
            require(trimmed.isNotBlank()) { "AppearanceProfileId cannot be blank." }
            return AppearanceProfileId(trimmed)
        }
    }
}
