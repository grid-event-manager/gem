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

    fun highHeader(sequence: Int, packetId: Int, flags: Int = 0): ByteArray = byteArrayOf(
        flags.toByte(),
        ((sequence ushr 24) and 0xFF).toByte(),
        ((sequence ushr 16) and 0xFF).toByte(),
        ((sequence ushr 8) and 0xFF).toByte(),
        (sequence and 0xFF).toByte(),
        0,
        (packetId and 0xFF).toByte(),
    )

    fun uuid(value: String): ByteArray =
        value.replace("-", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    fun zeroDecode(encoded: ByteArray): ByteArray {
        return LibomvZerocodeCodec.decode(encoded)
    }
}
