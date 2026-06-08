package org.hostess.protocol.libomv.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibomvPacketCodecTest {
    @Test
    fun `parses fixed packet ack sequence numbers`() {
        val ack = byteArrayOf(
            0,
            0,
            0,
            0,
            77,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            2,
        ) + u32(6) + u32(123456789)

        assertEquals(listOf(6L, 123456789L), LibomvPacketCodec.packetAckSequences(ack))
    }

    @Test
    fun `parses appended packet ack sequence numbers`() {
        val packetWithAppendedAcks = LibomvPacketTestBytes.highHeader(
            sequence = 77,
            packetId = 1,
            flags = 0x10,
        ) + byteArrayOf(7) + u32BigEndian(6) + u32BigEndian(123456789) + byteArrayOf(2)

        assertEquals(listOf(6L, 123456789L), LibomvPacketCodec.packetAckSequences(packetWithAppendedAcks))
    }

    @Test
    fun `rejects malformed packet ack`() {
        val ack = byteArrayOf(
            0,
            0,
            0,
            0,
            77,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            2,
        ) + u32(6)

        assertNull(LibomvPacketCodec.packetAckSequences(ack))
    }

    @Test
    fun `reports medium frequency packet labels`() {
        val packet = LibomvPacketTestBytes.mediumHeader(sequence = 88, packetId = 7) + byteArrayOf(1, 2, 3)

        assertEquals(SimulatorPacketType.UNKNOWN, LibomvPacketCodec.packetType(packet))
        assertEquals(7, LibomvPacketCodec.decodedPacketId(packet))
        assertEquals("medium_7", LibomvPacketCodec.decodedPacketLabel(packet))
    }

    @Test
    fun `keeps low and high packet ids in their own frequency families`() {
        val highStartPing = LibomvPacketTestBytes.highHeader(sequence = 89, packetId = 1) + byteArrayOf(3)
        val lowTestMessage = LibomvPacketTestBytes.lowHeader(sequence = 90, packetId = 1, flags = 0) +
            byteArrayOf(3)

        assertEquals(SimulatorPacketType.START_PING_CHECK, LibomvPacketCodec.packetType(highStartPing))
        assertEquals(SimulatorPacketType.UNKNOWN, LibomvPacketCodec.packetType(lowTestMessage))
        assertEquals("high_1", LibomvPacketCodec.decodedPacketLabel(highStartPing))
        assertEquals("low_1", LibomvPacketCodec.decodedPacketLabel(lowTestMessage))
    }

    @Test
    fun `reports known simulator packet names separately from raw packet labels`() {
        val alert = alertMessage("Group notice blocked")

        assertEquals(SimulatorPacketType.ALERT_MESSAGE, LibomvPacketCodec.packetType(alert))
        assertEquals("low_134", LibomvPacketCodec.decodedPacketLabel(alert))
        assertEquals("alert_message", LibomvPacketCodec.decodedPacketKnownName(alert))
    }

    @Test
    fun `parses alert message diagnostics`() {
        val alert = alertMessage("No permission for 33333333-3333-3333-3333-333333333333")

        val observation = LibomvPacketCodec.alertMessageObservation(alert)

        assertEquals(
            "No permission for 33333333-3333-3333-3333-333333333333",
            observation?.message,
        )
        assertEquals(1, observation?.alertInfoCount)
    }

    @Test
    fun `rejects malformed alert message diagnostics`() {
        val malformed = LibomvPacketTestBytes.lowHeader(sequence = 91, packetId = 134, flags = 0)

        assertNull(LibomvPacketCodec.alertMessageObservation(malformed))
    }

    @Test
    fun `regionHandshakeInfo detects true AgentAppearanceService fixture`() {
        val info = LibomvPacketCodec.regionHandshakeInfo(
            LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(regionProtocols = 1L),
        )

        assertEquals(true, info?.regionProtocolFlags?.agentAppearanceService)
    }

    @Test
    fun `regionHandshakeInfo ignores RegionInfo flags for false AgentAppearanceService fixture`() {
        val info = LibomvPacketCodec.regionHandshakeInfo(
            LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(
                regionInfoFlags = 1L,
                regionProtocols = 0L,
            ),
        )

        assertEquals(false, info?.regionProtocolFlags?.agentAppearanceService)
    }

    @Test
    fun `regionHandshakeInfo treats absent RegionInfo4 as unknown flags`() {
        val info = LibomvPacketCodec.regionHandshakeInfo(
            LibomvPacketTestBytes.regionHandshakeWithoutRegionInfo4(),
        )

        assertEquals(false, info?.regionProtocolFlags?.agentAppearanceService)
    }

    @Test
    fun `regionHandshakeInfo rejects malformed required RegionInfo body`() {
        assertNull(LibomvPacketCodec.regionHandshakeInfo(LibomvPacketTestBytes.malformedRegionHandshake()))
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun u32BigEndian(value: Long): ByteArray = byteArrayOf(
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun alertMessage(message: String): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 91, packetId = 134, flags = 0) +
            variable1(message) +
            byteArrayOf(1) +
            variable1("detail") +
            variable1("redacted")

    private fun variable1(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(bytes.size.toByte()) + bytes
    }
}
