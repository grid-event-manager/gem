package org.hostess.core.domain

@JvmInline
value class SecondLifeLoginName private constructor(val value: String) {
    val firstName: String
        get() = value.substringBefore(" ")

    val lastName: String
        get() = value.substringAfter(" ")

    companion object {
        fun fromUserInput(input: String): SecondLifeLoginNameResult {
            val normalized = input.trim()
                .replace('.', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
                .lowercase()

            if (normalized.isBlank()) {
                return SecondLifeLoginNameResult.Invalid(SecondLifeLoginNameInvalidReason.BLANK)
            }

            val withResident = if (' ' in normalized) normalized else "$normalized resident"
            return SecondLifeLoginNameResult.Valid(SecondLifeLoginName(withResident))
        }
    }
}

sealed interface SecondLifeLoginNameResult {
    data class Valid(val loginName: SecondLifeLoginName) : SecondLifeLoginNameResult
    data class Invalid(val reason: SecondLifeLoginNameInvalidReason) : SecondLifeLoginNameResult
}

enum class SecondLifeLoginNameInvalidReason {
    BLANK,
}
