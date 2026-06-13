package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.mapping.LibomvNoticePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ThreadedSimulatorSessionGatewayTest {
    @Test
    fun `serializes presence current groups ping ack heartbeat and close through one exchange`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                startPingCheck(7),
                agentMovementComplete(),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            gateway.sendCurrentGroupsRequest(circuit()),
        )
        waitUntil { exchange.sentNames().count { it == "agent_update" } >= 2 }
        gateway.close()

        assertEquals(null, result.redactedDetail)
        assertEquals(SIM_HOST, exchange.endpoint?.host)
        assertEquals(SIM_PORT, exchange.endpoint?.port)
        assertTrue(exchange.sentNames().contains("packet_ack"))
        assertTrue(exchange.sentNames().contains("complete_ping_check"))
        assertTrue(exchange.sentNames().contains("agent_data_update_request"))
        assertTrue(exchange.closed)
    }

    @Test
    fun `retries reliable notice and marks session failed when ack never arrives`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        val health = gateway.health(circuit())
        gateway.close()

        assertEquals("notice send ack timeout after 3 attempts", result.redactedMessage)
        assertEquals(3, exchange.sentNames().count { it == "improved_instant_message" })
        assertEquals(SimulatorSessionHealthStatus.FAILED, health.status)
    }

    @Test
    fun `rebuilds same circuit exchange after failed reliable notice before next send`() {
        val timedOutExchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val recoveredExchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                LibomvPacketCodec.packetAck(5),
            ),
        )
        val exchanges = mutableListOf(timedOutExchange, recoveredExchange)
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchanges.removeAt(0) })

        val timedOut = assertIs<SimulatorCircuitSendResult.Failed>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        val recovered = assertIs<SimulatorCircuitSendResult.Sent>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        gateway.close()

        assertEquals("notice send ack timeout after 3 attempts", timedOut.redactedMessage)
        assertEquals(3, timedOutExchange.sentNames().count { it == "improved_instant_message" })
        assertTrue(timedOutExchange.closed)
        assertTrue(recovered.redactedDetail.orEmpty().contains("transportAck=passed"))
        assertEquals(1, recoveredExchange.sentNames().count { it == "improved_instant_message" })
        assertTrue(recoveredExchange.sentNames().contains("use_circuit_code"))
    }

    @Test
    fun `keeps reading busy simulator traffic until delayed notice ack arrives`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                *Array(12) { index -> layerData(sequence = 200 + index) },
                LibomvPacketCodec.packetAck(5),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        gateway.close()

        assertTrue(result.redactedDetail.orEmpty().contains("transportAck=passed"))
        assertEquals(1, exchange.sentNames().count { it == "improved_instant_message" })
    }

    @Test
    fun `keeps reading past busy packet yield threshold until delayed notice ack arrives`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                *Array(140) { index -> layerData(sequence = 300 + index) },
                LibomvPacketCodec.packetAck(5),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        gateway.close()

        assertTrue(result.redactedDetail.orEmpty().contains("transportAck=passed"))
        assertEquals(1, exchange.sentNames().count { it == "improved_instant_message" })
    }

    @Test
    fun `waits through quiet simulator receive windows until delayed notice ack arrives`() {
        val exchange = ScriptedPacketExchange(
            inboundEvents = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                *Array(12) { null },
                LibomvPacketCodec.packetAck(5),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            gateway.sendNotice(circuit(), noticePacket()),
        )
        gateway.close()

        assertTrue(result.redactedDetail.orEmpty().contains("transportAck=passed"))
        assertEquals(1, exchange.sentNames().count { it == "improved_instant_message" })
    }

    @Test
    fun `caches out-of-order archive replies for later group requests`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                LibomvPacketCodec.packetAck(5),
                groupNoticesListReply(groupId = GROUP_ID_2),
                groupNoticesListReply(groupId = GROUP_ID),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val first = assertIs<SimulatorNoticeArchiveResult.Found>(
            gateway.requestGroupNoticeArchive(circuit(), GROUP_ID),
        )
        val second = assertIs<SimulatorNoticeArchiveResult.Found>(
            gateway.requestGroupNoticeArchive(circuit(), GROUP_ID_2),
        )
        gateway.close()

        assertEquals("Tonight", first.entries.single().subject)
        assertEquals("Tonight", second.entries.single().subject)
        assertEquals(1, exchange.sentNames().count { it == "group_notices_list_request" })
    }

    @Test
    fun `waits through quiet simulator receive windows until delayed archive reply arrives`() {
        val exchange = ScriptedPacketExchange(
            inboundEvents = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                LibomvPacketCodec.packetAck(5),
                *Array(16) { null },
                groupNoticesListReply(groupId = GROUP_ID),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val archive = assertIs<SimulatorNoticeArchiveResult.Found>(
            gateway.requestGroupNoticeArchive(circuit(), GROUP_ID),
        )
        gateway.close()

        assertEquals("Tonight", archive.entries.single().subject)
        assertEquals(1, exchange.sentNames().count { it == "group_notices_list_request" })
    }

    @Test
    fun `logs out and closes exchange after simulator logout reply`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                logoutReply(),
            ),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = gateway.logout(circuit())

        assertEquals(LoggedOut, result)
        assertTrue(exchange.sentNames().contains("logout_request"))
        assertTrue(exchange.closed)
    }

    @Test
    fun `sends close circuit and closes exchange when logout reply is absent`() {
        val exchange = ScriptedPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val gateway = ThreadedSimulatorSessionGateway(SimulatorPacketExchangeFactory { exchange })

        val result = gateway.logout(circuit())

        assertEquals(ClosedWithoutReply, result)
        assertTrue(exchange.sentNames().contains("logout_request"))
        assertTrue(exchange.sentNames().contains("close_circuit"))
        assertTrue(exchange.closed)
    }

    private class ScriptedPacketExchange(
        inboundPayloads: MutableList<ByteArray> = mutableListOf(),
        private val inboundEvents: MutableList<ByteArray?> =
            inboundPayloads.map<ByteArray, ByteArray?> { it }.toMutableList(),
    ) : SimulatorPacketExchange, AutoCloseable {
        private val lock = Any()
        var endpoint: SimulatorEndpoint? = null
            private set
        var closed: Boolean = false
            private set
        private val sentPayloads = mutableListOf<ByteArray>()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            synchronized(lock) {
                this.endpoint = endpoint
                sentPayloads += payloads
            }
        }

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            synchronized(lock) {
                if (inboundEvents.isEmpty()) {
                    null
                } else {
                    inboundEvents.removeAt(0)?.let { SimulatorInboundPacket(endpoint, it) }
                }
            }

        override fun close() {
            synchronized(lock) {
                closed = true
            }
        }

        fun sentNames(): List<String?> =
            synchronized(lock) {
                sentPayloads.map { LibomvPacketCodec.decodedPacketKnownName(it) }
            }
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(50) {
            if (condition()) {
                return
            }
            Thread.sleep(10)
        }
        assertTrue(condition())
    }

    private fun circuit(): SimulatorCircuit = SimulatorCircuit(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = SIM_HOST,
        simulatorPort = SIM_PORT,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private fun regionHandshake(): ByteArray =
        LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(regionProtocols = 1L)

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private fun startPingCheck(pingId: Int): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = 103, packetId = 1, flags = 0) +
            byteArrayOf(pingId.toByte()) +
            u32(0L)

    private fun layerData(sequence: Int): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = sequence, packetId = 11, flags = 0) + byteArrayOf(0)

    private fun logoutReply(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 104, packetId = 253, flags = 0) +
            LibomvPacketTestBytes.uuid(AGENT_ID) +
            LibomvPacketTestBytes.uuid(SESSION_ID)

    private fun groupNoticesListReply(
        groupId: String,
        noticeId: String = "99999999-9999-9999-9999-999999999999",
    ): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 104, packetId = 59, flags = 0x40) +
            LibomvPacketTestBytes.uuid(AGENT_ID) +
            LibomvPacketTestBytes.uuid(groupId) +
            byteArrayOf(1) +
            LibomvPacketTestBytes.uuid(noticeId) +
            u32(1_717_000_000L) +
            variable2("venue-proof") +
            variable2("Tonight") +
            byteArrayOf(1, 3)

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

    private fun variable2(value: String): ByteArray {
        val bytes = value.encodeToByteArray() + 0.toByte()
        return byteArrayOf(
            (bytes.size and 0xFF).toByte(),
            ((bytes.size ushr 8) and 0xFF).toByte(),
        ) + bytes
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val GROUP_ID_2 = "44444444-4444-4444-4444-444444444444"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
    }
}
