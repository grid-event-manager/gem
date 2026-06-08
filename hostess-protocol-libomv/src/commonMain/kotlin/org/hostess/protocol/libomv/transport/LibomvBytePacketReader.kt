package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvUuidCodec

internal class LibomvBytePacketReader(
    private val bytes: ByteArray,
    private var offset: Int,
) {
    fun readUuid(): String? =
        readBytes(ID_BYTES)?.let(LibomvUuidCodec::canonicalFromPacketBytes)

    fun readU32(): Long? {
        val values = readBytes(U32_BYTES) ?: return null
        return (values[0].toLong() and BYTE_MASK) or
            ((values[1].toLong() and BYTE_MASK) shl 8) or
            ((values[2].toLong() and BYTE_MASK) shl 16) or
            ((values[3].toLong() and BYTE_MASK) shl 24)
    }

    fun readU8(): Int? =
        readBytes(U8_BYTES)?.single()?.toInt()?.and(BYTE_MASK.toInt())

    fun readBool(): Boolean? =
        when (readU8()) {
            0 -> false
            1 -> true
            else -> null
        }

    fun readVariable1String(): String? {
        val size = readU8() ?: return null
        val value = readBytes(size) ?: return null
        return value.trimmedStringOrNull()
    }

    fun readVariable2String(): String? {
        val value = readVariable2Bytes() ?: return null
        return value.trimmedStringOrNull()
    }

    fun readVariable2Bytes(): ByteArray? {
        val size = readU16() ?: return null
        return readBytes(size)
    }

    fun skipBytes(size: Int): Boolean = readBytes(size) != null

    fun isExhausted(): Boolean = offset == bytes.size

    private fun ByteArray.trimmedStringOrNull(): String? {
        val trimmed = dropLastWhile { it == 0.toByte() }.toByteArray()
        return try {
            trimmed.decodeToString()
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    private fun readU16(): Int? {
        val values = readBytes(U16_BYTES) ?: return null
        return (values[0].toInt() and BYTE_MASK.toInt()) or
            ((values[1].toInt() and BYTE_MASK.toInt()) shl 8)
    }

    private fun readBytes(size: Int): ByteArray? {
        if (size < 0 || offset + size > bytes.size) {
            return null
        }
        val value = bytes.copyOfRange(offset, offset + size)
        offset += size
        return value
    }

    private companion object {
        const val ID_BYTES = 16
        const val U32_BYTES = 4
        const val U16_BYTES = 2
        const val U8_BYTES = 1
        const val BYTE_MASK = 0xFFL
    }
}
