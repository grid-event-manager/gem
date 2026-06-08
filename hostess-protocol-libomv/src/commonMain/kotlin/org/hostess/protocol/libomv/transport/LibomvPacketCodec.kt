package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvUuidCodec

internal object LibomvPacketCodec {
    fun useCircuitCode(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = USE_CIRCUIT_CODE,
        sequence = sequence,
        bodyLength = U32_BYTES + ID_BYTES + ID_BYTES,
    ) {
        writeBodyU32(circuit.circuitCode)
        writeUuid(circuit.sessionId)
        writeUuid(circuit.agentId)
    }

    fun completeAgentMovement(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = COMPLETE_AGENT_MOVEMENT,
        sequence = sequence,
        bodyLength = ID_BYTES + ID_BYTES + U32_BYTES,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
        writeBodyU32(circuit.circuitCode)
    }

    fun agentDataUpdateRequest(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = AGENT_DATA_UPDATE_REQUEST,
        sequence = sequence,
        bodyLength = ID_BYTES + ID_BYTES,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
    }

    fun groupNoticesListRequest(circuit: SimulatorCircuit, groupId: String, sequence: Int): ByteArray = lowPacket(
        packetId = GROUP_NOTICES_LIST_REQUEST,
        sequence = sequence,
        flags = RELIABLE_FLAGS,
        bodyLength = ID_BYTES + ID_BYTES + ID_BYTES,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
        writeUuid(groupId)
    }

    fun regionHandshakeReply(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = REGION_HANDSHAKE_REPLY,
        sequence = sequence,
        flags = RELIABLE_ZEROCODED_FLAGS,
        bodyLength = ID_BYTES + ID_BYTES + U32_BYTES,
        zerocode = true,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
        writeBodyU32(SELF_APPEARANCE_SUPPORT_FLAG)
    }

    fun completePingCheck(pingId: Int, sequence: Int): ByteArray = highPacket(
        packetId = COMPLETE_PING_CHECK,
        sequence = sequence,
        flags = 0,
        bodyLength = U8_BYTES,
    ) {
        writeByte(pingId)
    }

    fun agentUpdate(circuit: SimulatorCircuit, sequence: Int): ByteArray = highPacket(
        packetId = AGENT_UPDATE,
        sequence = sequence,
        flags = RELIABLE_ZEROCODED_FLAGS,
        bodyLength = AGENT_UPDATE_BODY_BYTES,
        zerocode = true,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
        writeQuaternionIdentity()
        writeQuaternionIdentity()
        writeByte(AGENT_STATE_WALKING)
        writeVector(128.0f, 128.0f, 20.0f)
        writeVector(0.0f, 1.0f, 0.0f)
        writeVector(1.0f, 0.0f, 0.0f)
        writeVector(0.0f, 0.0f, 1.0f)
        writeBodyF32(128.0f)
        writeBodyU32(AGENT_CONTROL_FLAGS_NONE)
        writeByte(AGENT_FLAGS_NONE)
    }

    fun packetAck(sequenceNumber: Long): ByteArray {
        val writer = LibomvBytePacketWriter(FIXED_HEADER_BYTES + FIXED_PACKET_ID_BYTES + U8_BYTES + U32_BYTES)
        writer.writeByte(0)
        writer.writeHeaderInt(0)
        writer.writeByte(0)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(PACKET_ACK_FIXED_ID)
        writer.writeByte(1)
        writer.writeBodyU32(sequenceNumber)
        return writer.toByteArray()
    }

    fun reliableSequenceNumber(payload: ByteArray): Long? {
        if (payload.size < FIXED_HEADER_BYTES || (payload[0].toInt() and RELIABLE_FLAGS) == 0) {
            return null
        }
        return ((payload[1].toLong() and BYTE_MASK_LONG) shl 24) +
            ((payload[2].toLong() and BYTE_MASK_LONG) shl 16) +
            ((payload[3].toLong() and BYTE_MASK_LONG) shl 8) +
            (payload[4].toLong() and BYTE_MASK_LONG)
    }

    fun packetType(payload: ByteArray): SimulatorPacketType =
        when (decodedPacket(payload)?.packetId) {
            REGION_HANDSHAKE -> SimulatorPacketType.REGION_HANDSHAKE
            START_PING_CHECK -> SimulatorPacketType.START_PING_CHECK
            AGENT_MOVEMENT_COMPLETE -> SimulatorPacketType.AGENT_MOVEMENT_COMPLETE
            GROUP_NOTICES_LIST_REPLY -> SimulatorPacketType.GROUP_NOTICES_LIST_REPLY
            GROUP_NOTICE_REQUESTED -> SimulatorPacketType.GROUP_NOTICE_REQUESTED
            IMPROVED_INSTANT_MESSAGE -> SimulatorPacketType.IMPROVED_INSTANT_MESSAGE
            else -> SimulatorPacketType.UNKNOWN
        }

    fun regionHandshakeInfo(payload: ByteArray): RegionHandshakeInfo? {
        val packet = decodedPacket(payload) ?: return null
        if (packet.packetId != REGION_HANDSHAKE) {
            return null
        }
        val reader = RegionHandshakeBodyReader(packet.decoded, packet.bodyOffset)
        return reader.regionProtocolFlags()?.let(::RegionHandshakeInfo)
    }

    fun packetAckSequences(payload: ByteArray): List<Long>? {
        appendedAckSequences(payload)?.let { return it }
        val decoded = try {
            LibomvZerocodeCodec.decode(payload)
        } catch (ex: IllegalArgumentException) {
            return null
        }
        if (decoded.size < FIXED_HEADER_BYTES + FIXED_PACKET_ID_BYTES + U8_BYTES) {
            return null
        }
        val extraLength = decoded[EXTRA_BYTES_OFFSET].toInt() and BYTE_MASK
        val markerOffset = FIXED_HEADER_BYTES + extraLength
        if (decoded.size < markerOffset + FIXED_PACKET_ID_BYTES + U8_BYTES) {
            return null
        }
        if (
            decoded[markerOffset] != LOW_FREQUENCY_MARKER.toByte() ||
            decoded[markerOffset + 1] != LOW_FREQUENCY_MARKER.toByte() ||
            decoded[markerOffset + 2] != LOW_FREQUENCY_MARKER.toByte() ||
            decoded[markerOffset + 3] != PACKET_ACK_FIXED_ID.toByte()
        ) {
            return null
        }
        val countOffset = markerOffset + FIXED_PACKET_ID_BYTES
        val count = decoded[countOffset].toInt() and BYTE_MASK
        val expectedSize = countOffset + U8_BYTES + (count * U32_BYTES)
        if (decoded.size != expectedSize) {
            return null
        }
        val reader = LibomvBytePacketReader(decoded, countOffset + U8_BYTES)
        return List(count) {
            reader.readU32() ?: return null
        }
    }

    private fun appendedAckSequences(payload: ByteArray): List<Long>? {
        if (payload.size < FIXED_HEADER_BYTES + U8_BYTES || (payload[0].toInt() and APPENDED_ACKS_FLAG) == 0) {
            return null
        }
        val count = payload.last().toInt() and BYTE_MASK
        val ackStart = payload.size - U8_BYTES - (count * U32_BYTES)
        if (count == 0 || ackStart < FIXED_HEADER_BYTES) {
            return null
        }
        return List(count) { index ->
            val offset = ackStart + (index * U32_BYTES)
            ((payload[offset].toLong() and BYTE_MASK_LONG) shl 24) +
                ((payload[offset + 1].toLong() and BYTE_MASK_LONG) shl 16) +
                ((payload[offset + 2].toLong() and BYTE_MASK_LONG) shl 8) +
                (payload[offset + 3].toLong() and BYTE_MASK_LONG)
        }
    }

    fun startPingId(payload: ByteArray): Int? {
        val packet = decodedPacket(payload) ?: return null
        if (packet.packetId != START_PING_CHECK || packet.decoded.size <= packet.bodyOffset) {
            return null
        }
        return packet.decoded[packet.bodyOffset].toInt() and BYTE_MASK
    }

    fun groupNoticesListReply(payload: ByteArray): SimulatorNoticeArchiveReply? {
        val packet = decodedPacket(payload) ?: return null
        if (packet.packetId != GROUP_NOTICES_LIST_REPLY) {
            return null
        }
        val reader = LibomvBytePacketReader(packet.decoded, packet.bodyOffset)
        reader.readUuid() ?: return null
        val groupId = reader.readUuid() ?: return null
        val entryCount = reader.readU8() ?: return null
        val entries = mutableListOf<SimulatorNoticeArchiveEntry>()
        repeat(entryCount) {
            val noticeId = reader.readUuid() ?: return null
            val timestamp = reader.readU32() ?: return null
            val fromName = reader.readVariable2String() ?: return null
            val subject = reader.readVariable2String() ?: return null
            val hasAttachment = reader.readBool() ?: return null
            val assetType = reader.readU8() ?: return null
            entries += SimulatorNoticeArchiveEntry(
                noticeId = noticeId,
                timestamp = timestamp,
                fromName = fromName,
                subject = subject,
                hasAttachment = hasAttachment,
                assetType = assetType,
            )
        }
        if (!reader.isExhausted()) {
            return null
        }
        return SimulatorNoticeArchiveReply(groupId = groupId, entries = entries)
    }

    private fun lowPacket(
        packetId: Int,
        sequence: Int,
        bodyLength: Int,
        flags: Int = 0,
        zerocode: Boolean = false,
        body: LibomvBytePacketWriter.() -> Unit,
    ): ByteArray {
        val writer = LibomvBytePacketWriter(LOW_HEADER_BYTES + bodyLength)
        writer.writeByte(flags)
        writer.writeHeaderInt(sequence)
        writer.writeByte(0)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte((packetId ushr 8) and BYTE_MASK)
        writer.writeByte(packetId and BYTE_MASK)
        writer.body()
        val packet = writer.toByteArray()
        return if (zerocode) LibomvZerocodeCodec.encode(packet) else packet
    }

    private fun highPacket(
        packetId: Int,
        sequence: Int,
        bodyLength: Int,
        flags: Int,
        zerocode: Boolean = false,
        body: LibomvBytePacketWriter.() -> Unit,
    ): ByteArray {
        val writer = LibomvBytePacketWriter(HIGH_HEADER_BYTES + bodyLength)
        writer.writeByte(flags)
        writer.writeHeaderInt(sequence)
        writer.writeByte(0)
        writer.writeByte(packetId)
        writer.body()
        val packet = writer.toByteArray()
        return if (zerocode) LibomvZerocodeCodec.encode(packet) else packet
    }

    private fun LibomvBytePacketWriter.writeUuid(value: String) {
        writeBytes(LibomvUuidCodec.packetBytes(value) ?: throw IllegalArgumentException("invalid uuid"))
    }

    private fun LibomvBytePacketWriter.writeQuaternionIdentity() {
        writeBodyF32(0.0f)
        writeBodyF32(0.0f)
        writeBodyF32(0.0f)
        writeBodyF32(1.0f)
    }

    private fun LibomvBytePacketWriter.writeVector(x: Float, y: Float, z: Float) {
        writeBodyF32(x)
        writeBodyF32(y)
        writeBodyF32(z)
    }

    private fun decodedPacket(payload: ByteArray): DecodedPacket? {
        val decoded = try {
            LibomvZerocodeCodec.decode(payload)
        } catch (ex: IllegalArgumentException) {
            return null
        }
        if (decoded.size < HIGH_HEADER_BYTES) {
            return null
        }
        val extraLength = decoded[EXTRA_BYTES_OFFSET].toInt() and BYTE_MASK
        val markerOffset = FIXED_HEADER_BYTES + extraLength
        if (decoded.size <= markerOffset) {
            return null
        }
        return when {
            decoded[markerOffset] == LOW_FREQUENCY_MARKER.toByte() &&
                decoded.size >= markerOffset + LOW_PACKET_ID_BYTES &&
                decoded[markerOffset + 1] == LOW_FREQUENCY_MARKER.toByte() -> DecodedPacket(
                    decoded = decoded,
                    packetId = ((decoded[markerOffset + 2].toInt() and BYTE_MASK) shl 8) +
                        (decoded[markerOffset + 3].toInt() and BYTE_MASK),
                    bodyOffset = markerOffset + LOW_PACKET_ID_BYTES,
                )
            decoded[markerOffset] == LOW_FREQUENCY_MARKER.toByte() -> null
            else -> DecodedPacket(
                decoded = decoded,
                packetId = decoded[markerOffset].toInt() and BYTE_MASK,
                bodyOffset = markerOffset + HIGH_PACKET_ID_BYTES,
            )
        }
    }

    private data class DecodedPacket(
        val decoded: ByteArray,
        val packetId: Int,
        val bodyOffset: Int,
    )

    private class RegionHandshakeBodyReader(
        private val bytes: ByteArray,
        private var offset: Int,
    ) {
        fun regionProtocolFlags(): RegionProtocolFlags? {
            if (isExhausted()) {
                return RegionProtocolFlags.unknown()
            }
            if (!skipRegionInfo() || !skipBytes(ID_BYTES) || !skipRegionInfo3()) {
                return null
            }
            return readRegionInfo4()
        }

        private fun skipRegionInfo(): Boolean =
            skipBytes(U32_BYTES) &&
                skipBytes(U8_BYTES) &&
                skipVariable1() &&
                skipBytes(ID_BYTES) &&
                skipBytes(U8_BYTES) &&
                skipBytes(F32_BYTES) &&
                skipBytes(F32_BYTES) &&
                skipBytes(ID_BYTES) &&
                skipBytes(ID_BYTES * TERRAIN_ID_FIELD_COUNT) &&
                skipBytes(F32_BYTES * TERRAIN_FLOAT_FIELD_COUNT)

        private fun skipRegionInfo3(): Boolean =
            skipBytes(S32_BYTES) &&
                skipBytes(S32_BYTES) &&
                skipVariable1() &&
                skipVariable1() &&
                skipVariable1()

        private fun readRegionInfo4(): RegionProtocolFlags {
            val count = readU8() ?: return RegionProtocolFlags.unknown()
            if (count == 0) {
                return RegionProtocolFlags.unknown()
            }
            readU64() ?: return RegionProtocolFlags.unknown()
            val regionProtocols = readU64() ?: return RegionProtocolFlags.unknown()
            return RegionProtocolFlags(
                agentAppearanceService = (regionProtocols and AGENT_APPEARANCE_SERVICE_FLAG) != 0L,
            )
        }

        private fun skipVariable1(): Boolean {
            val size = readU8() ?: return false
            return skipBytes(size)
        }

        private fun readU8(): Int? =
            readBytes(U8_BYTES)?.single()?.toInt()?.and(BYTE_MASK)

        private fun readU64(): Long? {
            val values = readBytes(U64_BYTES) ?: return null
            return values.foldIndexed(0L) { index, result, byte ->
                result or ((byte.toLong() and BYTE_MASK_LONG) shl (index * BYTE_BITS))
            }
        }

        private fun skipBytes(size: Int): Boolean = readBytes(size) != null

        private fun readBytes(size: Int): ByteArray? {
            if (size < 0 || offset + size > bytes.size) {
                return null
            }
            val value = bytes.copyOfRange(offset, offset + size)
            offset += size
            return value
        }

        private fun isExhausted(): Boolean = offset == bytes.size
    }

    private const val USE_CIRCUIT_CODE = 3
    private const val START_PING_CHECK = 1
    private const val COMPLETE_PING_CHECK = 2
    private const val AGENT_UPDATE = 4
    private const val REGION_HANDSHAKE = 148
    private const val REGION_HANDSHAKE_REPLY = 149
    private const val COMPLETE_AGENT_MOVEMENT = 249
    private const val AGENT_MOVEMENT_COMPLETE = 250
    private const val IMPROVED_INSTANT_MESSAGE = 254
    private const val AGENT_DATA_UPDATE_REQUEST = 386
    private const val GROUP_NOTICES_LIST_REQUEST = 58
    private const val GROUP_NOTICES_LIST_REPLY = 59
    private const val GROUP_NOTICE_REQUESTED = 465
    private const val PACKET_ACK_FIXED_ID = 0xFB
    private const val LOW_HEADER_BYTES = 10
    private const val HIGH_HEADER_BYTES = 7
    private const val FIXED_HEADER_BYTES = 6
    private const val FIXED_PACKET_ID_BYTES = 4
    private const val EXTRA_BYTES_OFFSET = 5
    private const val LOW_PACKET_ID_BYTES = 4
    private const val HIGH_PACKET_ID_BYTES = 1
    private const val ID_BYTES = 16
    private const val U32_BYTES = 4
    private const val S32_BYTES = 4
    private const val U64_BYTES = 8
    private const val U8_BYTES = 1
    private const val QUATERNION_BYTES = 16
    private const val VECTOR3_BYTES = 12
    private const val F32_BYTES = 4
    private const val AGENT_UPDATE_BODY_BYTES =
        ID_BYTES + ID_BYTES +
            QUATERNION_BYTES + QUATERNION_BYTES +
            U8_BYTES +
            VECTOR3_BYTES + VECTOR3_BYTES + VECTOR3_BYTES + VECTOR3_BYTES +
            F32_BYTES + U32_BYTES + U8_BYTES
    private const val LOW_FREQUENCY_MARKER = 0xFF
    private const val BYTE_MASK = 0xFF
    private const val BYTE_MASK_LONG = 0xFFL
    private const val BYTE_BITS = 8
    private const val TERRAIN_ID_FIELD_COUNT = 8
    private const val TERRAIN_FLOAT_FIELD_COUNT = 8
    private const val APPENDED_ACKS_FLAG = 0x10
    private const val RELIABLE_FLAGS = 0x40
    private const val RELIABLE_ZEROCODED_FLAGS = 0xC0
    private const val SELF_APPEARANCE_SUPPORT_FLAG = 4L
    private const val AGENT_APPEARANCE_SERVICE_FLAG = 1L
    private const val AGENT_CONTROL_FLAGS_NONE = 0L
    private const val AGENT_FLAGS_NONE = 0
    private const val AGENT_STATE_WALKING = 0
}
