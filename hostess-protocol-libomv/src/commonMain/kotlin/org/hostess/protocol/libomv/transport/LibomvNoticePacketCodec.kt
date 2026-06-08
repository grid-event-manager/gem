package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvUuidCodec

internal object LibomvNoticePacketCodec {
    fun improvedInstantMessage(packet: LibomvNoticePacket, sequence: Int): ByteArray {
        val fromAgentName = variableUtf8(packet.fromAgentName, VARIABLE_1_MAX)
        val message = variableUtf8(packet.message, VARIABLE_2_MAX)
        val binaryBucket = packet.binaryBucket.checkedBinaryBucket()
        val writer = LibomvBytePacketWriter(
            LOW_HEADER_BYTES +
                FIXED_FIELD_BYTES +
                VARIABLE_1_LENGTH_BYTES + fromAgentName.size +
                VARIABLE_2_LENGTH_BYTES + message.size +
                VARIABLE_2_LENGTH_BYTES + binaryBucket.size,
        )

        writer.writeByte(RELIABLE_ZEROCODED_FLAGS)
        writer.writeHeaderInt(sequence)
        writer.writeByte(NO_EXTRA_BYTES)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(IMPROVED_INSTANT_MESSAGE_PACKET_HIGH)
        writer.writeByte(IMPROVED_INSTANT_MESSAGE_PACKET_LOW)

        writer.writeUuid(packet.agentId)
        writer.writeUuid(packet.sessionId)
        writer.writeBool(packet.fromGroup)
        writer.writeUuid(packet.targetGroupId)
        writer.writeBodyU32(packet.parentEstateId.toUnsignedLong())
        writer.writeUuid(packet.regionId)
        writer.writeBodyF32(packet.position.x.toFloat())
        writer.writeBodyF32(packet.position.y.toFloat())
        writer.writeBodyF32(packet.position.z.toFloat())
        writer.writeByte(packet.offline)
        writer.writeByte(packet.dialog)
        writer.writeUuid(packet.instantMessageId)
        writer.writeBodyU32(packet.timestamp.toUnsignedLong())
        writer.writeVariable1(fromAgentName)
        writer.writeVariable2(message)
        writer.writeVariable2(binaryBucket)

        return LibomvZerocodeCodec.encode(writer.toByteArray())
    }

    private fun LibomvBytePacketWriter.writeUuid(value: String) {
        writeBytes(LibomvUuidCodec.packetBytes(value) ?: throw IllegalArgumentException("invalid uuid"))
    }

    private fun LibomvBytePacketWriter.writeBool(value: Boolean) {
        writeByte(if (value) TRUE else FALSE)
    }

    private fun LibomvBytePacketWriter.writeVariable1(value: ByteArray) {
        if (value.size > VARIABLE_1_MAX) {
            throw IllegalArgumentException("variable data too large")
        }
        writeByte(value.size)
        writeBytes(value)
    }

    private fun LibomvBytePacketWriter.writeVariable2(value: ByteArray) {
        if (value.size > VARIABLE_2_MAX) {
            throw IllegalArgumentException("variable data too large")
        }
        writeBodyU16(value.size)
        writeBytes(value)
    }

    private fun variableUtf8(value: String, maxLength: Int): ByteArray {
        val valueBytes = value.encodeToByteArray()
        if (valueBytes.size + NULL_TERMINATOR_BYTES > maxLength) {
            throw IllegalArgumentException("variable data too large")
        }
        return valueBytes + 0.toByte()
    }

    private fun ByteArray.checkedBinaryBucket(): ByteArray {
        if (size > VARIABLE_2_MAX) {
            throw IllegalArgumentException("variable data too large")
        }
        return this
    }

    private fun Int.toUnsignedLong(): Long =
        toLong() and UNSIGNED_32_MAX

    private const val RELIABLE_ZEROCODED_FLAGS = 0xC0
    private const val NO_EXTRA_BYTES = 0
    private const val LOW_FREQUENCY_MARKER = 0xFF
    private const val IMPROVED_INSTANT_MESSAGE_PACKET_HIGH = 0
    private const val IMPROVED_INSTANT_MESSAGE_PACKET_LOW = 254
    private const val LOW_HEADER_BYTES = 10
    private const val FIXED_FIELD_BYTES = 103
    private const val VARIABLE_1_LENGTH_BYTES = 1
    private const val VARIABLE_2_LENGTH_BYTES = 2
    private const val VARIABLE_1_MAX = 0xFF
    private const val VARIABLE_2_MAX = 0xFFFF
    private const val NULL_TERMINATOR_BYTES = 1
    private const val TRUE = 1
    private const val FALSE = 0
    private const val UNSIGNED_32_MAX = 0xFFFF_FFFFL
}
