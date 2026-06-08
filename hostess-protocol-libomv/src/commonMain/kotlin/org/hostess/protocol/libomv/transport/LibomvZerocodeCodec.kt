package org.hostess.protocol.libomv.transport

internal object LibomvZerocodeCodec {
    fun encode(unencoded: ByteArray): ByteArray {
        val headerLength = fixedHeaderLength(unencoded)
        val encoded = ByteArray(headerLength + (unencoded.size - headerLength) * ZERO_ENCODE_MAX_EXPANSION)
        unencoded.copyInto(encoded, destinationOffset = 0, startIndex = 0, endIndex = headerLength)
        var outputOffset = headerLength
        var zeroCount = 0

        for (index in headerLength until unencoded.size) {
            val value = unencoded[index]
            if (value == 0.toByte()) {
                zeroCount += 1
                if (zeroCount == ZERO_RUN_OVERFLOW) {
                    encoded[outputOffset++] = 0
                    encoded[outputOffset++] = BYTE_MASK.toByte()
                    zeroCount = 1
                }
            } else {
                if (zeroCount > 0) {
                    encoded[outputOffset++] = 0
                    encoded[outputOffset++] = zeroCount.toByte()
                    zeroCount = 0
                }
                encoded[outputOffset++] = value
            }
        }

        if (zeroCount > 0) {
            encoded[outputOffset++] = 0
            encoded[outputOffset++] = zeroCount.toByte()
        }

        return encoded.copyOf(outputOffset)
    }

    fun decode(encoded: ByteArray): ByteArray {
        if (encoded.size < FIXED_HEADER_BYTES || !encoded.hasFlag(MSG_ZEROCODED)) {
            return encoded.copyOf()
        }
        val headerLength = fixedHeaderLength(encoded)
        val encodedBodyEnd = encoded.bodyEndBeforeAppendedAcks(headerLength)
        val decoded = mutableListOf<Byte>()
        for (index in 0 until headerLength) {
            decoded += encoded[index]
        }

        var index = headerLength
        while (index < encodedBodyEnd) {
            val value = encoded[index]
            if (value == 0.toByte()) {
                if (index + 1 >= encodedBodyEnd) {
                    throw IllegalArgumentException("malformed zero run")
                }
                val count = encoded[index + 1].toInt() and BYTE_MASK
                repeat(count) { decoded += 0 }
                index += 2
            } else {
                decoded += value
                index += 1
            }
        }
        for (ackIndex in encodedBodyEnd until encoded.size) {
            decoded += encoded[ackIndex]
        }
        return decoded.toByteArray()
    }

    private fun fixedHeaderLength(packet: ByteArray): Int {
        require(packet.size >= FIXED_HEADER_BYTES)
        return FIXED_HEADER_BYTES + (packet[EXTRA_BYTES_OFFSET].toInt() and BYTE_MASK)
    }

    private fun ByteArray.bodyEndBeforeAppendedAcks(headerLength: Int): Int {
        if (!hasFlag(MSG_APPENDED_ACKS)) {
            return size
        }
        val appendedAckBytes = ((last().toInt() and BYTE_MASK) * ACK_SEQUENCE_BYTES) + ACK_COUNT_BYTES
        val bodyEnd = size - appendedAckBytes
        return if (bodyEnd < headerLength) headerLength else bodyEnd
    }

    private fun ByteArray.hasFlag(flag: Int): Boolean =
        (this[0].toInt() and flag) != 0

    private const val FIXED_HEADER_BYTES = 6
    private const val EXTRA_BYTES_OFFSET = 5
    private const val BYTE_MASK = 0xFF
    private const val ZERO_RUN_OVERFLOW = 0x100
    private const val ZERO_ENCODE_MAX_EXPANSION = 2
    private const val MSG_APPENDED_ACKS = 0x10
    private const val MSG_ZEROCODED = 0x80
    private const val ACK_SEQUENCE_BYTES = 4
    private const val ACK_COUNT_BYTES = 1
}
