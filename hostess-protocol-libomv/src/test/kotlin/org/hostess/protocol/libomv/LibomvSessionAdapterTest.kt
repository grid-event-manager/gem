package org.hostess.protocol.libomv

import java.time.Instant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.protocol.libomv.runtime.HostessHostIdentity
import org.hostess.protocol.libomv.runtime.HostessPlatformIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentityProvider
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvSessionAdapterTest {
    @Test
    fun `login routes through protocol runtime`() {
        val clientSession = LibomvClientSession.inactive()
        val adapter = LibomvSessionAdapter(
            clientSession = clientSession,
            loginRuntime = ProtocolLoginRuntime(
                clientSession = clientSession,
                httpClient = FailsIfCalledHttpClient,
                viewerIdentityProvider = viewerIdentityProvider(),
            ),
        )

        val result = adapter.login(loginRequest())

        val failure = assertIs<SessionLoginResult.Failure>(result).failure
        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("login secret unavailable", failure.redactedMessage)
    }

    @Test
    fun `logout routes through protocol runtime and clears matching session`() {
        val session = hostessSession()
        val clientSession = LibomvClientSession.active(session)
        val adapter = LibomvSessionAdapter(
            clientSession = clientSession,
            loginRuntime = ProtocolLoginRuntime(
                clientSession = clientSession,
                httpClient = FailsIfCalledHttpClient,
                viewerIdentityProvider = viewerIdentityProvider(),
            ),
        )

        val result = adapter.logout(session)

        assertEquals(SessionLogoutResult.LoggedOut, result)
        assertEquals("protocol session inactive", clientSession.requireSession(session)?.redactedMessage)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("venue-proof"),
        credentialHandle = CredentialHandle("proof-handle"),
    )

    private fun hostessSession(): HostessSession = HostessSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )

    private fun viewerIdentityProvider(): HostessViewerIdentityProvider = HostessViewerIdentityProvider {
        HostessViewerIdentity(
            channel = "Hostess",
            version = "0.1.0.0",
            author = "Hostess",
            platform = HostessPlatformIdentity("Linux", "6.8.0", "Linux 6.8.0 amd64 Test Runtime 17"),
            host = HostessHostIdentity(
                mac = "00000000000000000000000000000001",
                id0 = "00000000000000000000000000000002",
                hostId = "00000000000000000000000000000003",
            ),
        )
    }

    private object FailsIfCalledHttpClient : ProtocolHttpClient {
        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            error("HTTP must not be called by this adapter route proof")
        }
    }
}
