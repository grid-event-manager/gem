package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProtocolLibomvModuleTest {
    @Test
    fun `live runtime exposes one adapter set with shared session holder`() {
        val runtime = ProtocolLibomvModule.liveRuntime()

        assertTrue(runtime.protocolAvailable)
        assertTrue(runtime.clientSession.isProtocolAvailable())
        assertSame(runtime.clientSession, (runtime.sessionPort as LibomvSessionAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.groupPort as LibomvGroupAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.inventoryPort as LibomvInventoryAdapter).clientSession)
        assertSame(runtime.clientSession, (runtime.noticePort as LibomvNoticeAdapter).clientSession)
    }

    @Test
    fun `unavailable runtime composition still fails closed`() {
        val clientSession = LibomvClientSession.unavailable()
        val login = LibomvSessionAdapter(clientSession).login(loginRequest())

        assertFalse(clientSession.isProtocolAvailable())
        assertIs<SessionLoginResult.Failure>(login)
        assertEquals(CoreFailureReason.LOGIN_FAILED, login.failure.reason)
        assertEquals("protocol bootstrap unavailable", login.failure.redactedMessage)
    }

    @Test
    fun `live runtime unimplemented methods still fail closed`() {
        val runtime = ProtocolLibomvModule.liveRuntime()

        val login = runtime.sessionPort.login(loginRequest())
        val groups = runtime.groupPort.currentGroups(fakeInactiveSession())

        assertIs<SessionLoginResult.Failure>(login)
        assertEquals(CoreFailureReason.LOGIN_FAILED, login.failure.reason)
        assertEquals("protocol runtime unavailable", login.failure.redactedMessage)

        assertIs<GroupListResult.Failure>(groups)
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, groups.failure.reason)
        assertEquals("protocol runtime unavailable", groups.failure.redactedMessage)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("venue-proof"),
        credentialHandle = CredentialHandle("proof-handle"),
    )

    private fun fakeInactiveSession(): HostessSession = HostessSession(
        sessionId = SessionId("inactive-proof-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = false,
    )
}
