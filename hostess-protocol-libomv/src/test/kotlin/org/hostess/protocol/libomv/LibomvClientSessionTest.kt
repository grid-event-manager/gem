package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LibomvClientSessionTest {
    @Test
    fun `matching active Hostess session passes binding check`() {
        val session = hostessSession("live-session")
        val clientSession = LibomvClientSession.active(session)

        assertNull(clientSession.requireSession(session))
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
        startedAt = Instant.EPOCH,
        isActive = true,
    )
}
