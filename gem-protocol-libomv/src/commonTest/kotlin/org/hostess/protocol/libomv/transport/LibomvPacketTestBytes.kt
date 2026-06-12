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

    fun mediumHeader(sequence: Int, packetId: Int, flags: Int = 0): ByteArray = byteArrayOf(
        flags.toByte(),
        ((sequence ushr 24) and 0xFF).toByte(),
        ((sequence ushr 16) and 0xFF).toByte(),
        ((sequence ushr 8) and 0xFF).toByte(),
        (sequence and 0xFF).toByte(),
        0,
        0xFF.toByte(),
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

    fun regionHandshakeWithRegionProtocols(
        regionProtocols: Long,
        regionInfoFlags: Long = 0,
        regionName: String = "Test Region",
    ): ByteArray =
        LibomvZerocodeCodec.encode(
            lowHeader(sequence = REGION_HANDSHAKE_SEQUENCE, packetId = REGION_HANDSHAKE_PACKET_ID, flags = 0xC0) +
                regionHandshakeBody(
                    regionInfoFlags = regionInfoFlags,
                    regionProtocols = regionProtocols,
                    regionNameBytes = regionName.encodeToByteArray(),
                ),
        )

    fun regionHandshakeWithRawRegionName(
        regionNameBytes: ByteArray,
        regionProtocols: Long = 1L,
    ): ByteArray =
        LibomvZerocodeCodec.encode(
            lowHeader(sequence = REGION_HANDSHAKE_SEQUENCE, packetId = REGION_HANDSHAKE_PACKET_ID, flags = 0xC0) +
                regionHandshakeBody(
                    regionInfoFlags = 0,
                    regionProtocols = regionProtocols,
                    regionNameBytes = regionNameBytes,
                ),
        )

    fun regionHandshakeWithoutRegionInfo4(): ByteArray =
        LibomvZerocodeCodec.encode(
            lowHeader(sequence = REGION_HANDSHAKE_SEQUENCE, packetId = REGION_HANDSHAKE_PACKET_ID, flags = 0xC0) +
                regionHandshakeBody(
                    regionInfoFlags = 0,
                    regionProtocols = null,
                    regionNameBytes = "Test Region".encodeToByteArray(),
                ),
        )

    fun malformedRegionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            lowHeader(sequence = REGION_HANDSHAKE_SEQUENCE, packetId = REGION_HANDSHAKE_PACKET_ID, flags = 0xC0) +
                byteArrayOf(0),
        )

    private fun regionHandshakeBody(
        regionInfoFlags: Long,
        regionProtocols: Long?,
        regionNameBytes: ByteArray,
    ): ByteArray =
        u32(regionInfoFlags) +
            byteArrayOf(0) +
            variable1(regionNameBytes) +
            uuid(ZERO_ID) +
            byteArrayOf(0) +
            f32(20.0f) +
            f32(1.0f) +
            uuid(ZERO_ID) +
            terrainIds() +
            terrainFloats() +
            uuid(ZERO_ID) +
            s32(0) +
            s32(0) +
            variable1("") +
            variable1("") +
            variable1("") +
            regionInfo4(regionProtocols)

    private fun regionInfo4(regionProtocols: Long?): ByteArray =
        regionProtocols?.let { byteArrayOf(1) + u64(0) + u64(it) } ?: ByteArray(0)

    private fun terrainIds(): ByteArray =
        List(8) { uuid(ZERO_ID) }.fold(ByteArray(0), ByteArray::plus)

    private fun terrainFloats(): ByteArray =
        List(8) { f32(0.0f) }.fold(ByteArray(0), ByteArray::plus)

    private fun variable1(value: String): ByteArray =
        variable1(value.encodeToByteArray())

    private fun variable1(bytes: ByteArray): ByteArray {
        require(bytes.size <= 255)
        return byteArrayOf(bytes.size.toByte()) + bytes
    }

    private fun s32(value: Int): ByteArray = u32(value.toLong() and 0xFFFF_FFFFL)

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun u64(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 32) and 0xFF).toByte(),
        ((value ushr 40) and 0xFF).toByte(),
        ((value ushr 48) and 0xFF).toByte(),
        ((value ushr 56) and 0xFF).toByte(),
    )

    private fun f32(value: Float): ByteArray = u32(value.toBits().toLong() and 0xFFFF_FFFFL)

    private const val REGION_HANDSHAKE_SEQUENCE = 101
    private const val REGION_HANDSHAKE_PACKET_ID = 148
    private const val ZERO_ID = "00000000-0000-0000-0000-000000000000"
}
