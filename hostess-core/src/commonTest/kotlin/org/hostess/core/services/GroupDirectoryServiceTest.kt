package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.testing.FakeGroupPort
import org.hostess.core.testing.defaultSession

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
}
