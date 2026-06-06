package org.hostess.protocol.libomv

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class LibomvClientSessionTest {
    @Test
    fun `matching active Hostess session passes binding check`() {
        val session = hostessSession("live-session")
        val clientSession = activeSession(session)

        assertNull(clientSession.requireSession(session))
        val identity = assertIs<LibomvSessionIdentityResult.Success>(clientSession.requireIdentity(session)).identity
        assertEquals("agent-id", identity.agentId)
        assertEquals("live-session", identity.sessionId)
        assertEquals("seed-capability", identity.seedCapability)
        assertEquals("203.0.113.8", identity.simulatorIp)
        assertEquals(13000, identity.simulatorPort)
        assertEquals(123456789L, identity.regionHandle)
        assertEquals(987654321L, identity.circuitCode)
    }

    @Test
    fun `active session without protocol agent identity fails identity check`() {
        val session = hostessSession("live-session")
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            LibomvClientSession.active(session).requireIdentity(session),
        ).failure

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol agent identity unavailable", failure.redactedMessage)
    }

    @Test
    fun `active session without simulator identity fails identity check`() {
        val session = hostessSession("live-session")
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            LibomvClientSession.active(
                session = session,
                agentId = "agent-id",
                seedCapability = "seed-capability",
                simulatorIp = "203.0.113.8",
                simulatorPort = 13000,
                regionHandle = 123456789L,
            ).requireIdentity(session),
        ).failure

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol circuit identity unavailable", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("203.0.113.8"))
        assertFalse(failure.redactedMessage.orEmpty().contains("seed-capability"))
    }

    @Test
    fun `clear removes private identity fields`() {
        val oldSession = hostessSession("old-session")
        val newSession = hostessSession("new-session")
        val clientSession = activeSession(oldSession)

        clientSession.clear()
        clientSession.activate(newSession, agentId = "agent-id")

        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            clientSession.requireIdentity(newSession),
        ).failure
        assertEquals("protocol seed identity unavailable", failure.redactedMessage)
    }

    @Test
    fun `mismatched Hostess session fails without leaking IDs`() {
        val clientSession = LibomvClientSession.active(hostessSession("live-session"))
        val failure = clientSession.requireSession(hostessSession("other-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("hostess session mismatch", failure?.redactedMessage)
        assertFalse(failure?.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure?.redactedMessage.orEmpty().contains("other-session"))
    }

    @Test
    fun `inactive protocol session fails binding check`() {
        val failure = LibomvClientSession.inactive().requireSession(hostessSession("live-session"))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("protocol session inactive", failure?.redactedMessage)
    }

    @Test
    fun `inactive Hostess session fails binding check`() {
        val session = hostessSession("live-session")
        val failure = LibomvClientSession.active(session).requireSession(session.copy(isActive = false))

        assertEquals(CoreFailureReason.LOGIN_FAILED, failure?.reason)
        assertEquals("hostess session inactive", failure?.redactedMessage)
    }

    private fun hostessSession(id: String): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private fun activeSession(session: HostessSession): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = "agent-id",
        seedCapability = "seed-capability",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )
}
