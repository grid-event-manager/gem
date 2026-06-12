package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.core.domain.GroupMembership
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.testing.FakeGroupPort
import org.gem.core.testing.defaultSession

class GroupDirectoryServiceTest {
    @Test
    fun `simulator presence delegates through group port`() {
        val groupPort = FakeGroupPort()
        val session = defaultSession()

        val result = GroupDirectoryService(groupPort).simulatorPresence(session)

        assertIs<SimulatorPresenceProofResult.Success>(result)
        assertEquals(listOf(session), groupPort.presenceSessions)
        assertEquals(emptyList(), groupPort.sessions)
    }

    @Test
    fun `notice archive delegates through group port`() {
        val groupPort = FakeGroupPort()
        val session = defaultSession()
        val group = GroupMembership.fromValues("group", "Venue Hosts", true, true)

        val result = GroupDirectoryService(groupPort).noticeArchive(session, group)

        assertIs<GroupNoticeArchiveResult.Success>(result)
        assertEquals(listOf(session to group), groupPort.archiveRequests)
        assertEquals(emptyList(), groupPort.sessions)
    }
}
