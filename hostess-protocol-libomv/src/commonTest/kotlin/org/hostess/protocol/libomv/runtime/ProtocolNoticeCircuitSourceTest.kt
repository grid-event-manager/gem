package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvNoticePosition
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.LibomvPacketTestBytes
import org.hostess.protocol.libomv.transport.SimulatorEndpoint
import org.hostess.protocol.libomv.transport.SimulatorPacketSender
import org.hostess.protocol.libomv.transport.SimulatorPacketSequence
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolNoticeCircuitSourceTest {
    @Test
    fun `sends encoded notice packet through simulator circuit client`() {
        val sender = RecordingPacketSender()
        val source = ProtocolNoticeCircuitSource(
            ProtocolSimulatorCircuitClient(
                packetSender = sender,
                sequence = SimulatorPacketSequence(10),
            ),
        )

        val result = source.send(identity(), noticePacket())

        assertEquals(NoticeRuntimeResult.Sent, result)
        assertEquals(SIM_HOST, sender.endpoint?.host)
        assertEquals(SIM_PORT, sender.endpoint?.port)
        assertEquals(1, sender.payloads.size)
        assertContentEquals(
            LibomvPacketTestBytes.lowHeader(sequence = 11) +
                LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                byteArrayOf(0) +
                LibomvPacketTestBytes.uuid(GROUP_ID),
            LibomvPacketTestBytes.zeroDecode(sender.payloads.single()).copyOfRange(
                0,
                LOW_HEADER_AND_TARGET_BYTES,
            ),
        )
    }

    @Test
    fun `maps circuit send failure to notice runtime failure`() {
        val source = ProtocolNoticeCircuitSource(
            ProtocolSimulatorCircuitClient(RecordingPacketSender(failure = Exception("cannot reach $SIM_HOST"))),
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

    private class RecordingPacketSender(
        private val failure: Exception? = null,
    ) : SimulatorPacketSender {
        var endpoint: SimulatorEndpoint? = null
        var payloads: List<ByteArray> = emptyList()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            this.endpoint = endpoint
            this.payloads = payloads
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
