package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.llsd.LlsdValue
import kotlin.math.floor

internal object LibomvOsdLongParser {
    fun parse(value: LlsdValue?): Long = when (value) {
        is LlsdValue.ScalarValue -> parseScalar(value.value)
        else -> 0L
    }

    private fun parseScalar(text: String): Long {
        val number = text.trim()
            .takeIf(String::isNotBlank)
            ?.toDoubleOrNull()
            ?: return 0L
        if (number.isNaN() || number.isInfinite()) {
            return 0L
        }
        return floor(number).toLong()
    }
}
