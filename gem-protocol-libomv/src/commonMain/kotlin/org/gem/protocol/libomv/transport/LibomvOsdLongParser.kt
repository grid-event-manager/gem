package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.llsd.LlsdValue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.floor

internal object LibomvOsdLongParser {
    fun parse(value: LlsdValue?): Long = when (value) {
        is LlsdValue.ScalarValue -> parseScalar(value.value)
        else -> 0L
    }

    private fun parseScalar(text: String): Long {
        val trimmed = text.trim().takeIf(String::isNotBlank) ?: return 0L
        val number = trimmed.toDoubleOrNull()
            ?: return parseBinaryLong(trimmed) ?: 0L
        if (number.isNaN() || number.isInfinite()) {
            return 0L
        }
        return floor(number).toLong()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseBinaryLong(text: String): Long? {
        val bytes = try {
            Base64.decode(text)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (bytes.isEmpty() || bytes.size > Long.SIZE_BYTES) {
            return null
        }
        var value = 0L
        for (byte in bytes) {
            value = (value shl Byte.SIZE_BITS) or (byte.toLong() and 0xffL)
        }
        return value
    }
}
