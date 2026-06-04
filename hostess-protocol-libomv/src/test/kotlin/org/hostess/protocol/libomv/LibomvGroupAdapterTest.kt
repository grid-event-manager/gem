package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.GroupListResult
import org.hostess.protocol.libomv.runtime.CurrentGroupsFetchResult
import org.hostess.protocol.libomv.runtime.CurrentGroupsSource
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvGroupAdapterTest {
    @Test
    fun `current groups routes through protocol runtime`() {
        val session = hostessSession()
        val clientSession = LibomvClientSession.active(session)
        val adapter = LibomvGroupAdapter(
            clientSession = clientSession,
            groupRuntime = ProtocolGroupRuntime(
                clientSession = clientSession,
                currentGroupsSource = CurrentGroupsSource {
                    CurrentGroupsFetchResult.Success(
                        listOf(
                            LibomvGroupSnapshot(
                                groupId = "music",
                                displayName = "Music Room",
                                powers = LibomvMapping.SEND_NOTICES_POWER,
                                acceptsNotices = true,
                            ),
                        ),
                    )
                },
            ),
        )

        val group = assertIs<GroupListResult.Success>(adapter.currentGroups(session)).groups.single()
        assertEquals("music", group.groupId.value)
        assertEquals("Music Room", group.displayName.value)
    }

    @Test
    fun `current groups fallback still fails closed without runtime`() {
        val session = hostessSession()
        val adapter = LibomvGroupAdapter(clientSession = LibomvClientSession.active(session))

        val failure = assertIs<GroupListResult.Failure>(adapter.currentGroups(session)).failure

        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    private fun hostessSession(): HostessSession = HostessSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )
}
