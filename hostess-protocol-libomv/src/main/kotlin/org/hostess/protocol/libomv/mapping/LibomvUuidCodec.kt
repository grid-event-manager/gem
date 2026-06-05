package org.hostess.protocol.libomv.mapping

internal object LibomvUuidCodec {
    const val ZERO_UUID: String = "00000000-0000-0000-0000-000000000000"

    fun canonicalOrNull(value: String): String? =
        parseBytes(value)?.let(::format)

    fun xor(left: String, right: String): String? {
        val leftBytes = parseBytes(left) ?: return null
        val rightBytes = parseBytes(right) ?: return null
        return format(
            ByteArray(UUID_BYTES) { index ->
                (leftBytes[index].toInt() xor rightBytes[index].toInt()).toByte()
            },
        )
    }

    fun packetBytes(value: String): ByteArray? =
        parseBytes(value)

    private fun parseBytes(value: String): ByteArray? {
        if (value.length != UUID_TEXT_LENGTH) {
            return null
        }
        val hex = StringBuilder(UUID_HEX_LENGTH)
        value.forEachIndexed { index, char ->
            if (index.isHyphenPosition()) {
                if (char != '-') {
                    return null
                }
            } else {
                if (char.hexValue() == null) {
                    return null
                }
                hex.append(char)
            }
        }
        if (hex.length != UUID_HEX_LENGTH) {
            return null
        }
        return ByteArray(UUID_BYTES) { index ->
            val high = hex[index * 2].hexValue() ?: return null
            val low = hex[index * 2 + 1].hexValue() ?: return null
            ((high shl 4) or low).toByte()
        }
    }

    private fun format(bytes: ByteArray): String = buildString(UUID_TEXT_LENGTH) {
        bytes.forEachIndexed { index, byte ->
            if (index == 4 || index == 6 || index == 8 || index == 10) {
                append('-')
            }
            val value = byte.toInt() and BYTE_MASK
            append(HEX[value ushr 4])
            append(HEX[value and NIBBLE_MASK])
        }
    }

    private fun Int.isHyphenPosition(): Boolean =
        this == 8 || this == 13 || this == 18 || this == 23

    private fun Char.hexValue(): Int? = when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> null
    }

    private const val UUID_TEXT_LENGTH = 36
    private const val UUID_HEX_LENGTH = 32
    private const val UUID_BYTES = 16
    private const val BYTE_MASK = 0xFF
    private const val NIBBLE_MASK = 0x0F
    private const val HEX = "0123456789abcdef"
}
