package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvNoticePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProtocolSimulatorCircuitClientTest {
    @Test
    fun `delegates current groups through session gateway`() {
        val gateway = RecordingSimulatorSessionGateway(
            currentGroupsResult = SimulatorCircuitSendResult.Sent("queued"),
        )
        val client = ProtocolSimulatorCircuitClient(gateway)

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            client.sendCurrentGroupsRequest(circuit()),
        )

        assertEquals("queued", result.redactedDetail)
        assertEquals(listOf(circuit()), gateway.currentGroupsCircuits)
    }

    @Test
    fun `delegates notice packet through session gateway`() {
        val gateway = RecordingSimulatorSessionGateway(
            noticeResult = SimulatorCircuitSendResult.Sent("transportAck=passed"),
        )
        val client = ProtocolSimulatorCircuitClient(gateway)
        val packet = noticePacket()

        val result = assertIs<SimulatorCircuitSendResult.Sent>(
            client.sendNotice(circuit(), packet),
        )

        assertEquals("transportAck=passed", result.redactedDetail)
        assertEquals(listOf(circuit()), gateway.noticeCircuits)
        assertSame(packet, gateway.noticePackets.single())
    }

    @Test
    fun `canonicalizes archive group before delegating`() {
        val gateway = RecordingSimulatorSessionGateway(
            archiveResult = SimulatorNoticeArchiveResult.Found(
                listOf(
                    SimulatorNoticeArchiveEntry(
                        noticeId = NOTICE_ID,
                        timestamp = 1_717_000_000L,
                        fromName = "venue-proof",
                        subject = "Tonight",
                        hasAttachment = true,
                        assetType = 3,
                    ),
                ),
            ),
        )
        val client = ProtocolSimulatorCircuitClient(gateway)

        val result = assertIs<SimulatorNoticeArchiveResult.Found>(
            client.requestGroupNoticeArchive(circuit(), GROUP_ID.uppercase()),
        )

        assertEquals("Tonight", result.entries.single().subject)
        assertEquals(listOf(circuit()), gateway.archiveCircuits)
        assertEquals(listOf(GROUP_ID), gateway.archiveGroupIds)
    }

    @Test
    fun `rejects invalid archive group before gateway`() {
        val gateway = RecordingSimulatorSessionGateway()
        val client = ProtocolSimulatorCircuitClient(gateway)

        val result = assertIs<SimulatorNoticeArchiveResult.Failed>(
            client.requestGroupNoticeArchive(circuit(), "not-a-group-id"),
        )

        assertEquals(SimulatorNoticeArchiveStatus.REQUEST_INVALID, result.status)
        assertEquals("protocol simulator send failed", result.redactedMessage)
        assertTrue(gateway.archiveCircuits.isEmpty())
    }

    @Test
    fun `invalid circuit blocks all operations before gateway`() {
        val gateway = RecordingSimulatorSessionGateway()
        val client = ProtocolSimulatorCircuitClient(gateway)
        val invalidCircuit = circuit(simulatorIp = "not-an-ip")

        assertIs<SimulatorCircuitSendResult.Failed>(client.sendCurrentGroupsRequest(invalidCircuit))
        assertIs<SimulatorCircuitSendResult.Failed>(client.sendNotice(invalidCircuit, noticePacket()))
        assertIs<SimulatorPresenceResult.Failed>(client.ensurePresence(invalidCircuit))
        assertIs<SimulatorNoticeArchiveResult.Failed>(
            client.requestGroupNoticeArchive(invalidCircuit, GROUP_ID),
        )
        assertIs<Failed>(client.logout(invalidCircuit))
        assertEquals(SimulatorSessionHealthStatus.FAILED, client.health(invalidCircuit).status)

        assertTrue(gateway.currentGroupsCircuits.isEmpty())
        assertTrue(gateway.noticeCircuits.isEmpty())
        assertTrue(gateway.presenceCircuits.isEmpty())
        assertTrue(gateway.archiveCircuits.isEmpty())
        assertTrue(gateway.logoutCircuits.isEmpty())
        assertTrue(gateway.healthCircuits.isEmpty())
    }

    @Test
    fun `delegates presence logout and health through gateway`() {
        val gateway = RecordingSimulatorSessionGateway(
            presenceResult = SimulatorPresenceResult.Present(
                pingReplies = 2,
                cached = true,
                heartbeatActive = true,
            ),
            logoutResult = ClosedWithoutReply,
            healthResult = SimulatorSessionHealth(SimulatorSessionHealthStatus.PRESENT),
        )
        val client = ProtocolSimulatorCircuitClient(gateway)

        val presence = assertIs<SimulatorPresenceResult.Present>(client.ensurePresence(circuit()))
        val logout = client.logout(circuit())
        val health = client.health(circuit())

        assertEquals(2, presence.pingReplies)
        assertEquals(ClosedWithoutReply, logout)
        assertEquals(SimulatorSessionHealthStatus.PRESENT, health.status)
        assertEquals(listOf(circuit()), gateway.presenceCircuits)
        assertEquals(listOf(circuit()), gateway.logoutCircuits)
        assertEquals(listOf(circuit()), gateway.healthCircuits)
    }

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
        regionHandle = REGION_HANDLE,
        circuitCode = circuitCode,
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

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val NOTICE_ID = "99999999-9999-9999-9999-999999999999"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val REGION_HANDLE = 123456789L
        const val CIRCUIT_CODE = 0x01020304L
    }
}
