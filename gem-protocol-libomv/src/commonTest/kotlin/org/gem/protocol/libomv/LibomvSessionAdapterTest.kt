package org.gem.protocol.libomv

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.SessionId
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.protocol.libomv.runtime.HostessMachineIdentity
import org.gem.protocol.libomv.runtime.HostessMachineIdentityProvider
import org.gem.protocol.libomv.runtime.HostessHostIdentity
import org.gem.protocol.libomv.runtime.HostessPlatformIdentity
import org.gem.protocol.libomv.runtime.HostessViewerIdentity
import org.gem.protocol.libomv.runtime.HostessViewerIdentityProvider
import org.gem.protocol.libomv.runtime.JvmMd5DigestPort
import org.gem.protocol.libomv.runtime.LoginSecretResolver
import org.gem.protocol.libomv.runtime.ProtocolLoginRuntime
import org.gem.protocol.libomv.transport.ProtocolHttpClient
import org.gem.protocol.libomv.transport.ProtocolHttpRequest
import org.gem.protocol.libomv.transport.ProtocolHttpResponse
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.RecordingSimulatorSessionGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvSessionAdapterTest {
    @Test
    fun `login routes through protocol runtime`() {
        val clientSession = LibomvClientSession.inactive()
        val adapter = LibomvSessionAdapter(
            clientSession = clientSession,
            loginRuntime = protocolLoginRuntime(
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
        val session = gemSession()
        val clientSession = activeClientSession(session)
        val adapter = LibomvSessionAdapter(
            clientSession = clientSession,
            loginRuntime = protocolLoginRuntime(
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

    private fun gemSession(): GemSession = GemSession(
        sessionId = SessionId(SESSION_ID),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun activeClientSession(session: GemSession): LibomvClientSession =
        LibomvClientSession.active(
            session = session,
            agentId = AGENT_ID,
            seedCapability = "seed-capability",
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
        )

    private fun protocolLoginRuntime(
        clientSession: LibomvClientSession,
        httpClient: ProtocolHttpClient,
        viewerIdentityProvider: HostessViewerIdentityProvider,
    ): ProtocolLoginRuntime =
        ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = httpClient,
            circuitClient = ProtocolSimulatorCircuitClient(RecordingSimulatorSessionGateway()),
            viewerIdentityProvider = viewerIdentityProvider,
            secretResolver = LoginSecretResolver.unavailable(),
            clockPort = NoopClockPort,
            machineIdentityProvider = HostessMachineIdentityProvider {
                HostessMachineIdentity("08:00:27:DC:4A:9E", "08:00:27:DC:4A:9E")
            },
            digestPort = JvmMd5DigestPort,
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

    private object NoopClockPort : ClockPort {
        override fun now(): GemInstant = GemInstant.EPOCH

        override fun pause(duration: GemDelay) = Unit
    }

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
    }
}
