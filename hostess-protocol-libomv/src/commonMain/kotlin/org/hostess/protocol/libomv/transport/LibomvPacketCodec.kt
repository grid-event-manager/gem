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

    fun startPingId(payload: ByteArray): Int? {
        val packet = decodedPacket(payload) ?: return null
        if (packet.packetId != START_PING_CHECK || packet.decoded.size <= packet.bodyOffset) {
            return null
        }
        return packet.decoded[packet.bodyOffset].toInt() and BYTE_MASK
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
    private const val GROUP_NOTICES_LIST_REPLY = 464
    private const val GROUP_NOTICE_REQUESTED = 465
    private const val LOW_HEADER_BYTES = 10
    private const val HIGH_HEADER_BYTES = 7
    private const val FIXED_HEADER_BYTES = 6
    private const val EXTRA_BYTES_OFFSET = 5
    private const val LOW_PACKET_ID_BYTES = 4
    private const val HIGH_PACKET_ID_BYTES = 1
    private const val ID_BYTES = 16
    private const val U32_BYTES = 4
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
    private const val RELIABLE_ZEROCODED_FLAGS = 0xC0
    private const val SELF_APPEARANCE_SUPPORT_FLAG = 4L
    private const val AGENT_CONTROL_FLAGS_NONE = 0L
    private const val AGENT_FLAGS_NONE = 0
    private const val AGENT_STATE_WALKING = 0
}
