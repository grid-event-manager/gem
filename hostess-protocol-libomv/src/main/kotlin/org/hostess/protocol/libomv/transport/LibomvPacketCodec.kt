package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvUuidCodec

internal object LibomvPacketCodec {
    fun useCircuitCode(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = USE_CIRCUIT_CODE,
        sequence = sequence,
        bodyLength = U32_BYTES + UUID_BYTES + UUID_BYTES,
    ) {
        writeBodyU32(circuit.circuitCode)
        writeUuid(circuit.sessionId)
        writeUuid(circuit.agentId)
    }

    fun completeAgentMovement(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = COMPLETE_AGENT_MOVEMENT,
        sequence = sequence,
        bodyLength = UUID_BYTES + UUID_BYTES + U32_BYTES,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
        writeBodyU32(circuit.circuitCode)
    }

    fun agentDataUpdateRequest(circuit: SimulatorCircuit, sequence: Int): ByteArray = lowPacket(
        packetId = AGENT_DATA_UPDATE_REQUEST,
        sequence = sequence,
        bodyLength = UUID_BYTES + UUID_BYTES,
    ) {
        writeUuid(circuit.agentId)
        writeUuid(circuit.sessionId)
    }

    private fun lowPacket(
        packetId: Int,
        sequence: Int,
        bodyLength: Int,
        body: LibomvBytePacketWriter.() -> Unit,
    ): ByteArray {
        val writer = LibomvBytePacketWriter(LOW_HEADER_BYTES + bodyLength)
        writer.writeByte(0)
        writer.writeHeaderInt(sequence)
        writer.writeByte(0)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte(LOW_FREQUENCY_MARKER)
        writer.writeByte((packetId ushr 8) and BYTE_MASK)
        writer.writeByte(packetId and BYTE_MASK)
        writer.body()
        return writer.toByteArray()
    }

    private fun LibomvBytePacketWriter.writeUuid(value: String) {
        writeBytes(LibomvUuidCodec.packetBytes(value) ?: throw IllegalArgumentException("invalid uuid"))
    }

    private const val USE_CIRCUIT_CODE = 3
    private const val COMPLETE_AGENT_MOVEMENT = 249
    private const val AGENT_DATA_UPDATE_REQUEST = 386
    private const val LOW_HEADER_BYTES = 10
    private const val UUID_BYTES = 16
    private const val U32_BYTES = 4
    private const val LOW_FREQUENCY_MARKER = 0xFF
    private const val BYTE_MASK = 0xFF
}
