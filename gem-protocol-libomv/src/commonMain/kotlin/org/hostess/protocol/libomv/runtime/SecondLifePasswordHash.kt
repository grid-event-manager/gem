package org.hostess.protocol.libomv.runtime

internal class SecondLifePasswordHash private constructor(
    val wireValue: String,
) {
    companion object {
        private val HASH_PATTERN = Regex("\\$1\\$[0-9a-fA-F]{32}")

        fun fromSharedSecret(
            value: String,
            digestPort: Md5DigestPort,
        ): SecondLifePasswordHash? {
            if (value.isBlank()) {
                return null
            }
            if (value.startsWith("\$1\$")) {
                return value
                    .takeIf(HASH_PATTERN::matches)
                    ?.lowercase()
                    ?.let(::SecondLifePasswordHash)
            }
            if (value.length !in RAW_PASSWORD_LENGTH || value.any { it.code > ASCII_MAX }) {
                return null
            }
            return SecondLifePasswordHash("\$1\$${digestPort.md5Hex(value.encodeToByteArray())}")
        }

        private val RAW_PASSWORD_LENGTH = 1..16
        private const val ASCII_MAX = 0x7f
    }
}
