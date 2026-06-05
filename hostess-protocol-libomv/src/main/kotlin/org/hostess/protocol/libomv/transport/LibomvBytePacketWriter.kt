package org.hostess.protocol.libomv.transport

internal class LibomvBytePacketWriter(
    size: Int,
) {
    private val bytes = ByteArray(size)
    private var offset = 0

    fun writeByte(value: Int) {
        require(value in 0..BYTE_MASK)
        write(value.toByte())
    }

    fun writeHeaderInt(value: Int) {
        writeByte((value ushr 24) and BYTE_MASK)
        writeByte((value ushr 16) and BYTE_MASK)
        writeByte((value ushr 8) and BYTE_MASK)
        writeByte(value and BYTE_MASK)
    }

    fun writeBodyU32(value: Long) {
        require(value in 0..UNSIGNED_32_MAX)
        writeByte((value and BYTE_MASK.toLong()).toInt())
        writeByte(((value ushr 8) and BYTE_MASK.toLong()).toInt())
        writeByte(((value ushr 16) and BYTE_MASK.toLong()).toInt())
        writeByte(((value ushr 24) and BYTE_MASK.toLong()).toInt())
    }

    fun writeBytes(values: ByteArray) {
        values.forEach(::write)
    }

    fun toByteArray(): ByteArray {
        require(offset == bytes.size)
        return bytes.copyOf()
    }

    private fun write(value: Byte) {
        require(offset < bytes.size)
        bytes[offset] = value
        offset += 1
    }

    private companion object {
        const val BYTE_MASK = 0xFF
        const val UNSIGNED_32_MAX = 0xFFFF_FFFFL
    }
}
