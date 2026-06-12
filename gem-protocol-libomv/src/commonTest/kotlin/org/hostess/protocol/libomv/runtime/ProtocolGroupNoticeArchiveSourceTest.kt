package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.GroupMembership
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.RecordingSimulatorSessionGateway
import org.hostess.protocol.libomv.transport.SimulatorNoticeArchiveEntry
import org.hostess.protocol.libomv.transport.SimulatorNoticeArchiveResult
import org.hostess.protocol.libomv.transport.SimulatorNoticeArchiveStatus
import org.hostess.protocol.libomv.transport.toSimulatorCircuit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ProtocolGroupNoticeArchiveSourceTest {
    @Test
    fun `maps archive entries without exposing notice ids`() {
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
        val source = ProtocolGroupNoticeArchiveSource(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val result = assertIs<GroupNoticeArchiveResult.Success>(
            source.noticeArchive(identity(), group()),
        )

        val entry = result.entries.single()
        assertEquals("Tonight", entry.subject)
        assertEquals("venue-proof", entry.fromName)
        assertEquals(1_717_000_000L, entry.timestamp)
        assertEquals(true, entry.hasAttachment)
        assertEquals(3, entry.assetType)
        assertFalse(entry.toString().contains("99999999"))
        assertEquals(identity().toSimulatorCircuit(), gateway.archiveCircuits.single())
        assertEquals(GROUP_ID, gateway.archiveGroupIds.single())
    }

    @Test
    fun `maps archive proof gap to redacted failure`() {
        val gateway = RecordingSimulatorSessionGateway(
            archiveResult = SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REPLY_TIMEOUT,
                redactedMessage = "protocol simulator send failed",
            ),
        )
        val source = ProtocolGroupNoticeArchiveSource(
            ProtocolSimulatorCircuitClient(gateway),
        )

        val result = assertIs<GroupNoticeArchiveResult.Failure>(
            source.noticeArchive(identity(), group()),
        )

        assertEquals("notice archive proof_gap reply_timeout", result.failure.redactedMessage)
    }

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 0x01020304L,
    )

    private fun group(): GroupMembership =
        GroupMembership.fromValues(GROUP_ID, "Venue Hosts", true, true)

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"
        const val NOTICE_ID = "99999999-9999-9999-9999-999999999999"
    }
}
