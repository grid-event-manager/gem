package org.gem.core.appearance

class AppearanceColor private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AppearanceColor && value == other.value

    override fun hashCode(): Int =
        value.hashCode()

    override fun toString(): String =
        "AppearanceColor(value=$value)"

    companion object {
        fun from(input: String): AppearanceColorParseResult {
            val trimmed = input.trim()
            val raw = trimmed.removePrefix("#")
            val expanded = when (raw.length) {
                3 -> raw.flatMap { listOf(it, it) }.joinToString(separator = "")
                6 -> raw
                else -> return AppearanceColorParseResult.Invalid(input)
            }

            if (expanded.any { !it.isHexDigit() }) {
                return AppearanceColorParseResult.Invalid(input)
            }

            return AppearanceColorParseResult.Valid(AppearanceColor("#${expanded.uppercase()}"))
        }

        fun require(input: String): AppearanceColor =
            when (val parsed = from(input)) {
                is AppearanceColorParseResult.Valid -> parsed.color
                is AppearanceColorParseResult.Invalid -> throw IllegalArgumentException(
                    "AppearanceColor must be RGB, RRGGBB, #RGB, or #RRGGBB.",
                )
            }

        private fun Char.isHexDigit(): Boolean =
            this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}

sealed interface AppearanceColorParseResult {
    data class Valid(val color: AppearanceColor) : AppearanceColorParseResult
    data class Invalid(val rawValue: String) : AppearanceColorParseResult
}
