package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.LibomvMapping
import org.hostess.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolGroupRuntimeTest {
    @Test
    fun `current groups maps sendable group`() {
        val session = hostessSession()
        val result = runtime(
            session = session,
            groups = listOf(group("music", "Music Room", LibomvMapping.SEND_NOTICES_POWER, acceptsNotices = true)),
        ).currentGroups(session)

        val group = assertIs<GroupListResult.Success>(result).groups.single()
        assertEquals("music", group.groupId.value)
        assertEquals("Music Room", group.displayName.value)
        assertTrue(group.canSendNotices)
        assertEquals(true, group.acceptsNotices)
    }

    @Test
    fun `current groups maps non sendable group`() {
        val session = hostessSession()
        val result = runtime(
            session = session,
            groups = listOf(group("silent", "Silent Group", powers = 0L, acceptsNotices = false)),
        ).currentGroups(session)

        val group = assertIs<GroupListResult.Success>(result).groups.single()
        assertFalse(group.canSendNotices)
        assertEquals(false, group.acceptsNotices)
    }

    @Test
    fun `current groups preserves unknown notice preference`() {
        val session = hostessSession()
        val result = runtime(
            session = session,
            groups = listOf(group("unknown", "Unknown Preference", powers = 0L, acceptsNotices = null)),
        ).currentGroups(session)

        val group = assertIs<GroupListResult.Success>(result).groups.single()
        assertNull(group.acceptsNotices)
    }

    @Test
    fun `current groups allows duplicate display names`() {
        val session = hostessSession()
        val result = runtime(
            session = session,
            groups = listOf(
                group("music-1", "Music Room", LibomvMapping.SEND_NOTICES_POWER, acceptsNotices = true),
                group("music-2", "Music Room", LibomvMapping.SEND_NOTICES_POWER, acceptsNotices = true),
            ),
        ).currentGroups(session)

        val groups = assertIs<GroupListResult.Success>(result).groups
        assertEquals(listOf("music-1", "music-2"), groups.map { it.groupId.value })
        assertEquals(listOf("Music Room", "Music Room"), groups.map { it.displayName.value })
    }

    @Test
    fun `current groups fails duplicate IDs without leaking values`() {
        val session = hostessSession()
        val result = runtime(
            session = session,
            groups = listOf(
                group("music", "Music Room", LibomvMapping.SEND_NOTICES_POWER, acceptsNotices = true),
                group("music", "Music Annex", LibomvMapping.SEND_NOTICES_POWER, acceptsNotices = true),
            ),
        ).currentGroups(session)

        val failure = assertIs<GroupListResult.Failure>(result).failure
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, failure.reason)
        assertEquals("current groups invalid", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("music"))
    }

    @Test
    fun `current groups returns empty list without fake groups`() {
        val session = hostessSession()
        val result = runtime(session = session, groups = emptyList()).currentGroups(session)

        assertEquals(0, assertIs<GroupListResult.Success>(result).groups.size)
    }

    @Test
    fun `current groups rejects mismatched session without calling source`() {
        val runtime = ProtocolGroupRuntime(
            clientSession = activeClientSession(hostessSession("live-session")),
            currentGroupsSource = FailsIfCalledGroupsSource,
        )

        val result = runtime.currentGroups(hostessSession("other-session"))

        val failure = assertIs<GroupListResult.Failure>(result).failure
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, failure.reason)
        assertEquals("hostess session mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure.redactedMessage.orEmpty().contains("other-session"))
    }

    @Test
    fun `current groups maps source failure to redacted failure`() {
        val session = hostessSession()
        val runtime = ProtocolGroupRuntime(
            clientSession = activeClientSession(session),
            currentGroupsSource = CurrentGroupsSource {
                CurrentGroupsFetchResult.Failure(
                    status = CurrentGroupsFailureStatus.TRANSPORT_GAP,
                    redactedMessage = "current groups unavailable",
                )
            },
        )

        val result = runtime.currentGroups(session)

        val failure = assertIs<GroupListResult.Failure>(result).failure
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, failure.reason)
        assertEquals("current groups unavailable", failure.redactedMessage)
    }

    @Test
    fun `current groups requires complete live identity before calling source`() {
        val session = hostessSession()
        val runtime = ProtocolGroupRuntime(
            clientSession = LibomvClientSession.active(session),
            currentGroupsSource = FailsIfCalledGroupsSource,
        )

        val result = runtime.currentGroups(session)

        val failure = assertIs<GroupListResult.Failure>(result).failure
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, failure.reason)
        assertEquals("protocol agent identity unavailable", failure.redactedMessage)
    }

    @Test
    fun `simulator presence delegates through presence source`() {
        val session = hostessSession()
        val runtime = ProtocolGroupRuntime(
            clientSession = activeClientSession(session),
            simulatorPresenceSource = SimulatorPresenceSource {
                SimulatorPresenceProofResult.Success(presenceProof())
            },
        )

        val result = assertIs<SimulatorPresenceProofResult.Success>(runtime.simulatorPresence(session))

        assertEquals(SimulatorPresenceProofStatus.PASSED, result.proof.simulatorPresenceStatus)
        assertEquals(SimulatorPresenceProofStatus.PASSED, result.proof.agentUpdateStatus)
    }

    @Test
    fun `simulator presence rejects mismatched session without calling source`() {
        val runtime = ProtocolGroupRuntime(
            clientSession = activeClientSession(hostessSession("live-session")),
            simulatorPresenceSource = FailsIfCalledPresenceSource,
        )

        val result = runtime.simulatorPresence(hostessSession("other-session"))

        val failure = assertIs<SimulatorPresenceProofResult.Failure>(result)
        assertEquals(SimulatorPresenceProofStatus.BLOCKED, failure.proof.simulatorPresenceStatus)
        assertEquals("hostess session mismatch", failure.failure.redactedMessage)
    }

    private fun runtime(
        session: HostessSession,
        groups: List<LibomvGroupSnapshot>,
    ): ProtocolGroupRuntime = ProtocolGroupRuntime(
        clientSession = activeClientSession(session),
        currentGroupsSource = CurrentGroupsSource { CurrentGroupsFetchResult.Success(groups) },
    )

    private fun activeClientSession(session: HostessSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = AGENT_ID,
        seedCapability = "seed-capability",
        simulatorIp = SIM_HOST,
        simulatorPort = SIM_PORT,
        regionHandle = REGION_HANDLE,
        circuitCode = CIRCUIT_CODE,
    )

    private fun group(
        id: String,
        displayName: String,
        powers: Long,
        acceptsNotices: Boolean?,
    ): LibomvGroupSnapshot = LibomvGroupSnapshot(
        groupId = id,
        displayName = displayName,
        powers = powers,
        acceptsNotices = acceptsNotices,
    )

    private fun hostessSession(id: String = "live-session"): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private object FailsIfCalledGroupsSource : CurrentGroupsSource {
        override fun currentGroups(identity: LibomvSessionIdentity): CurrentGroupsFetchResult {
            error("group source must not be called for a rejected session")
        }
    }

    private object FailsIfCalledPresenceSource : SimulatorPresenceSource {
        override fun simulatorPresence(identity: LibomvSessionIdentity): SimulatorPresenceProofResult {
            error("presence source must not be called for a rejected session")
        }
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SIM_HOST = "203.0.113.8"
        const val SIM_PORT = 13000
        const val REGION_HANDLE = 123456789L
        const val CIRCUIT_CODE = 0x01020304L

        fun presenceProof(): SimulatorPresenceProof = SimulatorPresenceProof(
            simulatorPresenceStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeStatus = SimulatorPresenceProofStatus.PASSED,
            regionHandshakeReplyStatus = SimulatorPresenceProofStatus.PASSED,
            agentMovementStatus = SimulatorPresenceProofStatus.PASSED,
            agentUpdateStatus = SimulatorPresenceProofStatus.PASSED,
        )
    }
}
