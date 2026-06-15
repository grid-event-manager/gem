package org.gem.core.domain

object SecondLifeLoginSecretPolicy {
    private val HASH_PATTERN = Regex("\\$1\\$[0-9a-fA-F]{32}")
    private val SECOND_LIFE_RAW_PASSWORD_LENGTH = 1..16
    private const val SECOND_LIFE_RAW_PASSWORD_MAX_CODEPOINT = 0x7f

    fun normalizeForStorage(value: String): String? {
        val normalized = value.trim()
        if (normalized.isBlank()) {
            return null
        }
        if (normalized.startsWith("\$1\$")) {
            return normalized
                .takeIf(HASH_PATTERN::matches)
                ?.lowercase()
        }
        if (
            normalized.length !in SECOND_LIFE_RAW_PASSWORD_LENGTH ||
            normalized.any { it.code > SECOND_LIFE_RAW_PASSWORD_MAX_CODEPOINT }
        ) {
            return null
        }
        return normalized
    }
}
