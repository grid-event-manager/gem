package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvNoticePosition
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtocolSimulatorCircuitClientTest {
    @Test
    fun `sends current-groups only after MetaBolt-shaped presence sequence`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete(), simulatorPacketAck(6)),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = client.sendCurrentGroupsRequest(circuit())

        assertEquals(SimulatorCircuitSendResult.Sent, result)
        assertEquals(SIM_HOST, exchange.sent.first().endpoint.host)
        assertEquals(SIM_PORT, exchange.sent.first().endpoint.port)
        val payloads = exchange.sentPayloads()
        assertEquals(6, payloads.size)
        assertLowPacket(
            payload = payloads[0],
            sequence = 1,
            packetId = 3,
            body = u32(CIRCUIT_CODE) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                LibomvPacketTestBytes.uuid(AGENT_ID),
        )
        assertPacketAck(payloads[1], ackedSequence = 101)
        assertLowPacket(
            payload = payloads[2],
            sequence = 2,
            packetId = 149,
            flags = 0xC0,
            body = LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                u32(4L),
        )
        assertLowPacket(
            payload = payloads[3],
            sequence = 3,
            packetId = 249,
            body = LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                u32(CIRCUIT_CODE),
        )
        assertHighPacketPrefix(
            payload = payloads[4],
            sequence = 4,
            packetId = 4,
            flags = 0xC0,
            bodyPrefix = LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID),
        )
        assertLowPacket(
            payload = payloads[5],
            sequence = 5,
            packetId = 386,
            body = LibomvPacketTestBytes.uuid(AGENT_ID) + LibomvPacketTestBytes.uuid(SESSION_ID),
        )
    }

    @Test
    fun `answers simulator pings while waiting for presence packets`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                startPingCheck(pingId = 7),
                regionHandshake(),
                startPingCheck(pingId = 8),
                agentMovementComplete(),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = client.sendCurrentGroupsRequest(circuit())

        assertEquals(SimulatorCircuitSendResult.Sent, result)
        val payloads = exchange.sentPayloads()
        assertHighPacketPrefix(
            payload = payloads[1],
            sequence = 2,
            packetId = 2,
            bodyPrefix = byteArrayOf(7),
        )
        assertPacketAck(payloads[2], ackedSequence = 101)
        assertLowPacket(payloads[3], sequence = 3, packetId = 149, flags = 0xC0)
        assertLowPacket(payloads[4], sequence = 4, packetId = 249)
        assertHighPacketPrefix(
            payload = payloads[5],
            sequence = 5,
            packetId = 2,
            bodyPrefix = byteArrayOf(8),
        )
        assertHighPacketPrefix(payloads[6], sequence = 6, packetId = 4, flags = 0xC0)
        assertLowPacket(payloads[7], sequence = 7, packetId = 386)
    }

    @Test
    fun `triggers movement before handshake when login circuit sends traffic first`() {
        val preHandshakeTraffic = List(12) { layerData(sequence = 200 + it) }
        val exchange = RecordingPacketExchange(
            inboundPayloads = (
                preHandshakeTraffic +
                    regionHandshake() +
                    agentMovementComplete()
                ).toMutableList(),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = client.sendCurrentGroupsRequest(circuit())

        assertEquals(SimulatorCircuitSendResult.Sent, result)
        val payloads = exchange.sentPayloads()
        assertEquals(6, payloads.size)
        assertLowPacket(payloads[0], sequence = 1, packetId = 3)
        assertLowPacket(payloads[1], sequence = 2, packetId = 249)
        assertPacketAck(payloads[2], ackedSequence = 101)
        assertLowPacket(payloads[3], sequence = 3, packetId = 149, flags = 0xC0)
        assertHighPacketPrefix(payloads[4], sequence = 4, packetId = 4, flags = 0xC0)
        assertLowPacket(payloads[5], sequence = 5, packetId = 386)
    }

    @Test
    fun `ensurePresence returns parsed region protocol flags from fresh handshake`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(regionProtocols = 1L),
                agentMovementComplete(),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorPresenceResult.Present>(client.ensurePresence(circuit()))

        assertFalse(result.cached)
        assertTrue(result.regionProtocolFlags.agentAppearanceService)
    }

    @Test
    fun `ensurePresence returns cached region protocol flags on later calls`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(regionProtocols = 1L),
                agentMovementComplete(),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        assertIs<SimulatorPresenceResult.Present>(client.ensurePresence(circuit()))
        val cached = assertIs<SimulatorPresenceResult.Present>(client.ensurePresence(circuit()))

        assertTrue(cached.cached)
        assertTrue(cached.regionProtocolFlags.agentAppearanceService)
    }

    @Test
    fun `malformed region handshake fails redacted as malformed`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(LibomvPacketTestBytes.malformedRegionHandshake()),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorPresenceResult.Failed>(client.ensurePresence(circuit()))

        assertEquals(SimulatorPresenceStatus.HANDSHAKE_MALFORMED, result.status)
        assertEquals("protocol simulator send failed", result.redactedMessage)
        assertFalse(result.redactedMessage.contains(AGENT_ID))
    }

    @Test
    fun `reuses established presence for later notice on same circuit`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete(), simulatorPacketAck(6)),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        assertEquals(SimulatorCircuitSendResult.Sent, client.sendCurrentGroupsRequest(circuit()))
        assertEquals(SimulatorCircuitSendResult.Sent, client.sendNotice(circuit(), noticePacket()))

        val payloads = exchange.sentPayloads()
        assertEquals(1, payloads.count { packetId(it) == 3 })
        assertEquals(1, payloads.count { packetId(it) == 249 })
        assertLowPacket(payloads[0], sequence = 1, packetId = 3)
        assertPacketAck(payloads[1], ackedSequence = 101)
        assertLowPacket(payloads[3], sequence = 3, packetId = 249)
        assertLowPacket(payloads.last(), sequence = 6, packetId = 254, flags = 0xC0)
    }

    @Test
    fun `notice send fails when simulator does not acknowledge reliable packet`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            client.sendNotice(circuit(), noticePacket()),
        )

        assertEquals("protocol simulator send failed", result.redactedMessage)
        val noticePayloads = exchange.sentPayloads().filter { packetId(it) == 254 }
        assertEquals(3, noticePayloads.size)
        noticePayloads.forEach { payload ->
            assertLowPacket(payload, sequence = 5, packetId = 254, flags = 0xC0)
        }
    }

    @Test
    fun `requests group notice archive after MetaBolt-shaped presence sequence`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                groupNoticesListReply(),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorNoticeArchiveResult.Found>(
            client.requestGroupNoticeArchive(circuit(), GROUP_ID),
        )

        val entry = result.entries.single()
        assertEquals("Tonight", entry.subject)
        assertEquals("venue-proof", entry.fromName)
        assertEquals(1_717_000_000L, entry.timestamp)
        assertEquals(true, entry.hasAttachment)
        assertEquals(3, entry.assetType)
        val archiveRequest = exchange.sentPayloads().first { packetId(it) == 58 }
        assertLowPacket(
            payload = archiveRequest,
            sequence = 5,
            packetId = 58,
            flags = 0x40,
            body = LibomvPacketTestBytes.uuid(AGENT_ID) +
                LibomvPacketTestBytes.uuid(SESSION_ID) +
                LibomvPacketTestBytes.uuid(GROUP_ID),
        )
    }

    @Test
    fun `caches out-of-order group notice archive replies for later group reads`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                groupNoticesListReply(groupId = GROUP_ID_2, noticeId = "77777777-7777-7777-7777-777777777777"),
                groupNoticesListReply(groupId = GROUP_ID),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val first = assertIs<SimulatorNoticeArchiveResult.Found>(
            client.requestGroupNoticeArchive(circuit(), GROUP_ID),
        )
        val second = assertIs<SimulatorNoticeArchiveResult.Found>(
            client.requestGroupNoticeArchive(circuit(), GROUP_ID_2),
        )

        assertEquals("Tonight", first.entries.single().subject)
        assertEquals("Tonight", second.entries.single().subject)
        assertEquals(1, exchange.sentPayloads().count { packetId(it) == 58 })
    }

    @Test
    fun `rejects archive reply for a different group`() {
        val exchange = RecordingPacketExchange(
            inboundPayloads = mutableListOf(
                regionHandshake(),
                agentMovementComplete(),
                groupNoticesListReply(groupId = "55555555-5555-5555-5555-555555555555"),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorNoticeArchiveResult.Failed>(
            client.requestGroupNoticeArchive(circuit(), GROUP_ID),
        )

        assertEquals(SimulatorNoticeArchiveStatus.WRONG_GROUP_REPLY, result.status)
        assertEquals("protocol simulator send failed", result.redactedMessage)
    }

    @Test
    fun `handshake timeout prevents current-groups packet send`() {
        val exchange = RecordingPacketExchange()
        val client = ProtocolSimulatorCircuitClient(
            packetExchange = exchange,
            sequence = SimulatorPacketSequence(0),
        )

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            client.sendCurrentGroupsRequest(circuit()),
        )

        assertEquals("protocol simulator send failed", result.redactedMessage)
        assertEquals(1, exchange.sentPayloads().size)
        assertLowPacket(exchange.sentPayloads().single(), sequence = 1, packetId = 3)
    }

    @Test
    fun `send failures are redacted`() {
        val exchange = RecordingPacketExchange(
            failure = Exception("cannot reach $SIM_HOST:$SIM_PORT for $AGENT_ID"),
        )
        val client = ProtocolSimulatorCircuitClient(packetExchange = exchange)

        val result = assertIs<SimulatorCircuitSendResult.Failed>(
            client.sendCurrentGroupsRequest(circuit()),
        )

        assertEquals("protocol simulator send failed", result.redactedMessage)
        assertFalse(result.redactedMessage.contains(SIM_HOST))
        assertFalse(result.redactedMessage.contains(AGENT_ID))
        assertFalse(result.redactedMessage.contains(CIRCUIT_CODE.toString()))
    }

    @Test
    fun `invalid circuit fields fail before datagram send`() {
        val cases = listOf(
            circuit(agentId = "not-a-uuid"),
            circuit(sessionId = "not-a-uuid"),
            circuit(seedCapability = ""),
            circuit(simulatorIp = ""),
            circuit(simulatorIp = "not-a-host"),
            circuit(simulatorIp = "999.0.113.8"),
            circuit(simulatorPort = 0),
            circuit(simulatorPort = 65536),
            circuit(circuitCode = 0),
            circuit(circuitCode = 0x1_0000_0000L),
        )

        for (case in cases) {
            val exchange = RecordingPacketExchange()
            val client = ProtocolSimulatorCircuitClient(packetExchange = exchange)

            val result = assertIs<SimulatorCircuitSendResult.Failed>(
                client.sendCurrentGroupsRequest(case),
            )

            assertEquals("protocol simulator send failed", result.redactedMessage)
            assertEquals(0, exchange.sent.size)
        }
    }

    private fun assertLowPacket(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        flags: Int = 0,
        body: ByteArray = ByteArray(0),
    ) {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        val header = LibomvPacketTestBytes.lowHeader(
            sequence = sequence,
            packetId = packetId,
            flags = flags,
        )
        assertContentEquals(header + body, decoded.copyOfRange(0, header.size + body.size))
    }

    private fun assertHighPacketPrefix(
        payload: ByteArray,
        sequence: Int,
        packetId: Int,
        flags: Int = 0,
        bodyPrefix: ByteArray = ByteArray(0),
    ) {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        val expected = LibomvPacketTestBytes.highHeader(sequence, packetId, flags) + bodyPrefix
        assertContentEquals(expected, decoded.copyOfRange(0, expected.size))
    }

    private fun assertPacketAck(payload: ByteArray, ackedSequence: Long) {
        val expected = byteArrayOf(
            0,
            0,
            0,
            0,
            0,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            1,
        ) + u32(ackedSequence)
        assertContentEquals(expected, payload)
    }

    private fun packetId(payload: ByteArray): Int {
        val decoded = LibomvPacketTestBytes.zeroDecode(payload)
        return if (decoded[6] == 0xFF.toByte()) {
            ((decoded[8].toInt() and 0xFF) shl 8) + (decoded[9].toInt() and 0xFF)
        } else {
            decoded[6].toInt() and 0xFF
        }
    }

    private fun u32(value: Long): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    private fun circuit(
        agentId: String = AGENT_ID,
        sessionId: String = SESSION_ID,
        seedCapability: String = "seed-capability",
        simulatorIp: String = SIM_HOST,
        simulatorPort: Int = SIM_PORT,
        circuitCode: Long = CIRCUIT_CODE,
    ): SimulatorCircuit = SimulatorCircuit(
        agentId = agentId,
        sessionId = sessionId,
        seedCapability = seedCapability,
        simulatorIp = simulatorIp,
        simulatorPort = simulatorPort,
        regionHandle = 123456789L,
        circuitCode = circuitCode,
    )

    private fun regionHandshake(): ByteArray =
        LibomvZerocodeCodec.encode(
            LibomvPacketTestBytes.lowHeader(sequence = 101, packetId = 148, flags = 0xC0),
        )

    private fun agentMovementComplete(): ByteArray =
        LibomvPacketTestBytes.lowHeader(sequence = 102, packetId = 250, flags = 0)

    private fun startPingCheck(pingId: Int): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = 103, packetId = 1, flags = 0) +
            byteArrayOf(pingId.toByte()) +
            u32(0L)

    private fun layerData(sequence: Int): ByteArray =
        LibomvPacketTestBytes.highHeader(sequence = sequence, packetId = 11, flags = 0) +
            byteArrayOf(0)

    private fun groupNoticesListReply(
        groupId: String = GROUP_ID,
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

    private fun simulatorPacketAck(ackedSequence: Long): ByteArray =
        byteArrayOf(
            0,
            0,
            0,
            0,
            105,
            0,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFB.toByte(),
            1,
        ) + u32(ackedSequence)

    private fun noticePacket(): LibomvNoticePacket = LibomvNoticePacket(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        fromGroup = false,
        targetGroupId = GROUP_ID,
        fromAgentName = "venue-proof",
        message = "Gig tonight|Doors at 8",
        dialog = 32,
        offline = 0,
        instantMessageId = "44444444-4444-4444-4444-444444444444",
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

    private class RecordingPacketExchange(
        private val inboundPayloads: MutableList<ByteArray> = mutableListOf(),
        private val failure: Exception? = null,
    ) : SimulatorPacketExchange {
        val sent = mutableListOf<SentDatagram>()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            failure?.let { throw it }
            sent += SentDatagram(endpoint, payloads)
        }

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            if (inboundPayloads.isEmpty()) {
                null
            } else {
                SimulatorInboundPacket(endpoint, inboundPayloads.removeAt(0))
            }

        fun sentPayloads(): List<ByteArray> = sent.flatMap { it.payloads }
    }

    private data class SentDatagram(
        val endpoint: SimulatorEndpoint,
        val payloads: List<ByteArray>,
    )

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val GROUP_ID_2 = "55555555-5555-5555-5555-555555555555"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val CIRCUIT_CODE = 0x01020304L
    }
}
