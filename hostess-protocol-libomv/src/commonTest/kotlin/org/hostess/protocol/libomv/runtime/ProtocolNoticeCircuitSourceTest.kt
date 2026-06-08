package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvNoticePosition
import org.hostess.protocol.libomv.transport.LibomvZerocodeCodec
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.LibomvPacketTestBytes
import org.hostess.protocol.libomv.transport.SimulatorEndpoint
import org.hostess.protocol.libomv.transport.SimulatorInboundPacket
import org.hostess.protocol.libomv.transport.SimulatorPacketExchange
import org.hostess.protocol.libomv.transport.SimulatorPacketSequence
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolNoticeCircuitSourceTest {
    @Test
    fun `sends encoded notice packet through simulator circuit client`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete(), simulatorPacketAck(15)),
        )
        val source = ProtocolNoticeCircuitSource(
            ProtocolSimulatorCircuitClient(
                packetExchange = exchange,
                sequence = SimulatorPacketSequence(10),
            ),
        )

        val result = source.send(identity(), noticePacket())

        assertIs<NoticeRuntimeResult.Sent>(result)
        assertEquals(SIM_HOST, exchange.endpoint?.host)
        assertEquals(SIM_PORT, exchange.endpoint?.port)
        assertEquals(6, exchange.payloads.size)
        assertContentEquals(
            LibomvPacketTestBytes.lowHeader(sequence = 15) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                byteArrayOf(0) +
                LibomvPacketTestBytes.uuid(GROUP_ID),
            LibomvPacketTestBytes.zeroDecode(exchange.payloads.last()).copyOfRange(
                0,
                LOW_HEADER_AND_TARGET_BYTES,
            ),
        )
    }

    @Test
    fun `maps circuit send failure to notice runtime failure`() {
        val source = ProtocolNoticeCircuitSource(
            ProtocolSimulatorCircuitClient(RecordingPacketExchange(failure = Exception("cannot reach $SIM_HOST"))),
        )

        val result = assertIs<NoticeRuntimeResult.Failed>(source.send(identity(), noticePacket()))

        assertEquals("protocol simulator send failed", result.message)
    }

    private fun identity(
        simulatorIp: String = SIM_HOST,
    ): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = simulatorIp,
        simulatorPort = SIM_PORT,
        regionHandle = 123456789L,
        circuitCode = CIRCUIT_CODE,
    )

    private fun noticePacket(): LibomvNoticePacket = LibomvNoticePacket(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        fromGroup = false,
        targetGroupId = GROUP_ID,
        fromAgentName = "venue-proof",
        message = "Gig tonight|Doors at 8",
        dialog = 32,
        offline = 0,
        instantMessageId = "22222222-2222-2222-2222-222222222222",
        parentEstateId = 0,
        timestamp = 0,
        regionId = "00000000-0000-0000-0000-000000000000",
        position = LibomvNoticePosition.ZERO,
        attachment = null,
        binaryBucket = ByteArray(0),
    )

    private fun regionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 101, packetId = 148, flags = 0xC0),
        )

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private fun simulatorPacketAck(ackedSequence: Long): ByteArray =
        byteArrayOf(
            0,
            0,
            0,
            0,
            103,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            1,
            (ackedSequence and 0xFF).toByte(),
            ((ackedSequence ushr 8) and 0xFF).toByte(),
            ((ackedSequence ushr 16) and 0xFF).toByte(),
            ((ackedSequence ushr 24) and 0xFF).toByte(),
        )

    private class RecordingPacketExchange(
        private val inboundPayloads: MutableList<ByteArray> = mutableListOf(),
        private val failure: Exception? = null,
    ) : SimulatorPacketExchange {
        var endpoint: SimulatorEndpoint? = null
        var payloads: List<ByteArray> = emptyList()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            this.endpoint = endpoint
            this.payloads = this.payloads + payloads
        }

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            if (inboundPayloads.isEmpty()) {
                null
            } else {
                SimulatorInboundPacket(endpoint, inboundPayloads.removeAt(0))
            }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val CIRCUIT_CODE = 987654321L
        const val LOW_HEADER_AND_TARGET_BYTES = 59
    }
}
