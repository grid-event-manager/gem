package org.gem.protocol.libomv.transport

internal object UnsignedLongBitsParser {
    fun parse(text: String): Long? =
        text.toLongOrNull() ?: parseUnsigned(text)

    private fun parseUnsigned(text: String): Long? {
        val unsignedText = text.removePrefix("+").takeIf(String::isNotEmpty) ?: return null
        if (unsignedText.any { it !in '0'..'9' }) {
            return null
        }
        val comparable = unsignedText.trimStart('0').ifEmpty { "0" }
        if (comparable.length > UNSIGNED_LONG_MAX.length ||
            comparable.length == UNSIGNED_LONG_MAX.length && comparable > UNSIGNED_LONG_MAX
        ) {
            return null
        }
        var bits = 0L
        unsignedText.forEach { char ->
            bits = bits * 10 + (char - '0')
        }
        return bits
    }

    private const val UNSIGNED_LONG_MAX = "18446744073709551615"
}
