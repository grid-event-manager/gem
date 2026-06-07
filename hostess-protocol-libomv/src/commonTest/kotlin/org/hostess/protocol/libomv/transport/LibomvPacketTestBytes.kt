package org.hostess.protocol.libomv.transport

internal object LibomvPacketTestBytes {
    fun lowHeader(sequence: Int, packetId: Int = 254, flags: Int = 0xC0): ByteArray = byteArrayOf(
        flags.toByte(),
        ((sequence ushr 24) and 0xFF).toByte(),
        ((sequence ushr 16) and 0xFF).toByte(),
        ((sequence ushr 8) and 0xFF).toByte(),
        (sequence and 0xFF).toByte(),
        0,
        0xFF.toByte(),
        0xFF.toByte(),
        ((packetId ushr 8) and 0xFF).toByte(),
        (packetId and 0xFF).toByte(),
    )

    fun uuid(value: String): ByteArray =
        value.replace("-", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    fun zeroDecode(encoded: ByteArray): ByteArray {
        val headerLength = 6 + (encoded[5].toInt() and 0xFF)
        val decoded = mutableListOf<Byte>()
        for (index in 0 until headerLength) {
            decoded += encoded[index]
        }
        var index = headerLength
        while (index < encoded.size) {
            val value = encoded[index]
            if (value == 0.toByte()) {
                val count = encoded[index + 1].toInt() and 0xFF
                repeat(count) { decoded += 0 }
                index += 2
            } else {
                decoded += value
                index += 1
            }
        }
        return decoded.toByteArray()
    }
}
