package org.hostess.protocol.libomv.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentityResult
import org.hostess.protocol.libomv.mapping.LoginKeys
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolLoginRuntimeTest {
    @Test
    fun `login fails closed when handle cannot resolve`() {
        val httpClient = RecordingHttpClient()
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login secret unavailable", result.failure.redactedMessage)
        assertFalse(httpClient.called)
        assertFalse(result.failure.redactedMessage.orEmpty().contains("proof-handle"))
    }

    @Test
    fun `login posts source-derived LLSD and activates session on success`() {
        val clientSession = LibomvClientSession.inactive()
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = httpClient,
            secretResolver = LoginSecretResolver { resolvedSecret() },
            clock = Clock.fixed(Instant.parse("2026-06-04T20:00:00Z"), ZoneOffset.UTC),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        val session = assertIs<SessionLoginResult.Success>(result).session
        assertEquals("live-session", session.sessionId.value)
        assertEquals("venue-proof", session.accountLabel.value)
        assertEquals(Instant.parse("2026-06-04T20:00:00Z"), session.startedAt)
        assertTrue(session.isActive)
        assertNull(clientSession.requireSession(session))
        val identity = assertIs<LibomvSessionIdentityResult.Success>(clientSession.requireIdentity(session)).identity
        assertEquals("agent-id", identity.agentId)
        assertEquals(secureUrl("caps.example", "/private"), identity.seedCapability)
        assertEquals("203.0.113.8", identity.simulatorIp)
        assertEquals(13000, identity.simulatorPort)
        assertEquals((1024L shl 32) or 2048L, identity.regionHandle)
        assertEquals(123456789L, identity.circuitCode)

        val request = httpClient.capturedRequest
        assertEquals("POST", request?.method)
        assertEquals(loginUrl(), request?.url)
        val body = assertIs<ProtocolHttpBody.TextBody>(request?.body)
        assertTrue(body.content.contains("<key>${LoginKeys.SECRET}</key>"))
        assertTrue(body.content.contains("<key>first</key><string>Venue</string>"))
        assertTrue(body.content.contains("<key>last</key><string>Host</string>"))
        assertFalse(body.content.contains("proof-handle"))
    }

    @Test
    fun `login maps transport failure to redacted failure`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = ThrowingHttpClient(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login transport failed", result.failure.redactedMessage)
    }

    @Test
    fun `login maps malformed response to redacted failure`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = RecordingHttpClient("<llsd><map>".encodeToByteArray()),
            secretResolver = LoginSecretResolver { resolvedSecret() },
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login failed", result.failure.redactedMessage)
    }

    @Test
    fun `login can pass while later identity fails without simulator fields`() {
        val clientSession = LibomvClientSession.inactive()
        val runtime = ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = RecordingHttpClient(successBody("live-session", includeSimulatorFields = false)),
            secretResolver = LoginSecretResolver { resolvedSecret() },
        )

        val login = runtime.login(loginRequest("proof-handle"))

        val session = assertIs<SessionLoginResult.Success>(login).session
        val failure = assertIs<LibomvSessionIdentityResult.Failure>(
            clientSession.requireIdentity(session),
        ).failure
        assertEquals(CoreFailureReason.LOGIN_FAILED, failure.reason)
        assertEquals("protocol simulator identity unavailable", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure.redactedMessage.orEmpty().contains("caps.example"))
    }

    @Test
    fun `logout clears matching session`() {
        val session = hostessSession("live-session")
        val clientSession = LibomvClientSession.active(session)
        val runtime = ProtocolLoginRuntime(clientSession, RecordingHttpClient())

        val result = runtime.logout(session)

        assertEquals(SessionLogoutResult.LoggedOut, result)
        assertEquals("protocol session inactive", clientSession.requireSession(session)?.redactedMessage)
    }

    @Test
    fun `logout rejects mismatched session without leaking IDs`() {
        val clientSession = LibomvClientSession.active(hostessSession("live-session"))
        val runtime = ProtocolLoginRuntime(clientSession, RecordingHttpClient())

        val result = runtime.logout(hostessSession("other-session"))

        val failure = assertIs<SessionLogoutResult.Failure>(result).failure
        assertEquals(CoreFailureReason.LOGOUT_FAILED, failure.reason)
        assertEquals("hostess session mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure.redactedMessage.orEmpty().contains("other-session"))
    }

    private fun loginRequest(handle: String): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("venue-proof"),
        credentialHandle = CredentialHandle(handle),
    )

    private fun resolvedSecret(): LoginSecret = LoginSecret(
        loginUri = loginUrl(),
        firstName = "Venue",
        lastName = "Host",
        sharedSecret = "resolved-secret",
    )

    private fun hostessSession(id: String): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )

    private fun successBody(
        sessionValue: String,
        includeSimulatorFields: Boolean = true,
    ): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "true")
        field(LoginKeys.AGENT_ID, "agent-id")
        field(LoginKeys.SESSION_ID, sessionValue)
        field(LoginKeys.SEED_CAPABILITY, secureUrl("caps.example", "/private"))
        if (includeSimulatorFields) {
            field(LoginKeys.SIM_IP, "203.0.113.8")
            integer(LoginKeys.SIM_PORT, "13000")
            integer(LoginKeys.REGION_X, "1024")
            integer(LoginKeys.REGION_Y, "2048")
            integer(LoginKeys.CIRCUIT_CODE, "123456789")
        }
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(key).append("</key><string>").append(value).append("</string>")
    }

    private fun StringBuilder.integer(key: String, value: String) {
        append("<key>").append(key).append("</key><integer>").append(value).append("</integer>")
    }

    private class RecordingHttpClient(
        private val body: ByteArray = ByteArray(0),
    ) : ProtocolHttpClient {
        var capturedRequest: ProtocolHttpRequest? = null
        val called: Boolean
            get() = capturedRequest != null

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            capturedRequest = request
            return ProtocolHttpResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = body,
                redactedSummary = "POST ${secureUrl("login.example", "/<redacted>")} -> 200",
            )
        }
    }

    private class ThrowingHttpClient : ProtocolHttpClient {
        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            throw ProtocolHttpException("redacted transport failure")
        }
    }

    private companion object {
        fun loginUrl(): String = secureUrl("login.example", "/agent")

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
