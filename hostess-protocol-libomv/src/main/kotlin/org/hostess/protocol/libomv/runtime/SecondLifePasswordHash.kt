package org.hostess.protocol.libomv.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal class SecondLifePasswordHash private constructor(
    val wireValue: String,
) {
    companion object {
        private val HASH_PATTERN = Regex("\\$1\\$[0-9a-fA-F]{32}")

        fun fromSharedSecret(value: String): SecondLifePasswordHash? {
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
            return SecondLifePasswordHash("\$1\$${md5AsciiHex(value)}")
        }

        private fun md5AsciiHex(value: String): String =
            MessageDigest.getInstance("MD5")
                .digest(value.toByteArray(StandardCharsets.US_ASCII))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        private val RAW_PASSWORD_LENGTH = 1..16
        private const val ASCII_MAX = 0x7f
    }
}
