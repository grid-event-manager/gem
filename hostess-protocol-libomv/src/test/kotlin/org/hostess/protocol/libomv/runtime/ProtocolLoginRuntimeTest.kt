package org.hostess.protocol.libomv.runtime

import java.time.Clock
import java.time.Duration
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
            viewerIdentityProvider = FailsIfCalledViewerIdentityProvider,
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login secret unavailable", result.failure.redactedMessage)
        assertFalse(httpClient.called)
        assertFalse(result.failure.redactedMessage.orEmpty().contains("proof-handle"))
    }

    @Test
    fun `login posts source-derived xml rpc request and activates session from llsd response`() {
        val clientSession = LibomvClientSession.inactive()
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = httpClient,
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            clock = Clock.fixed(Instant.parse("2026-06-04T20:00:00Z"), ZoneOffset.UTC),
            machineIdentityProvider = machineIdentityProvider(),
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

        val request = httpClient.capturedRequest ?: error("login request was not captured")
        assertEquals("POST", request.method)
        assertEquals(loginUrl(), request.url)
        assertEquals("text/xml", request.headers["Content-Type"])
        assertEquals(Duration.ofSeconds(120), request.timeout)
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body)
        assertEquals("text/xml", body.contentType)
        val normalized = LoginPackageCaptureNormalizer.normalize(body.content)
        assertEquals("login_to_simulator", normalized.methodName)
        assertEquals(NormalizedLoginString("string", "Venue"), normalized.fields["first"])
        assertEquals(NormalizedLoginString("string", "Host"), normalized.fields["last"])
        assertEquals(NormalizedLoginString("string", "last"), normalized.fields["start"])
        assertEquals(NormalizedLoginString("string", "Hostess"), normalized.fields[LoginKeys.CHANNEL])
        assertEquals(NormalizedLoginString("string", "0.1.0.0"), normalized.fields[LoginKeys.VERSION])
        assertEquals(
            NormalizedLoginString("string", "Linux 6.8.0 amd64 Test Runtime 17"),
            normalized.fields[LoginKeys.PLATFORM],
        )
        assertTrue(assertIs<NormalizedLoginString>(normalized.fields[LoginKeys.SECRET]).value.matches(SECOND_LIFE_HASH_PATTERN))
        assertEquals(NormalizedLoginString("string", "08:00:27:DC:4A:9E"), normalized.fields[LoginKeys.MAC])
        assertEquals(NormalizedLoginString("string", "08:00:27:DC:4A:9E"), normalized.fields[LoginKeys.ID0])
        assertEquals(NormalizedLoginString("string", "true"), normalized.fields[LoginKeys.AGREE_TO_TOS])
        assertEquals(NormalizedLoginString("string", "true"), normalized.fields[LoginKeys.READ_CRITICAL])
        assertEquals(NormalizedLoginInteger("i4", 0), normalized.fields["last_exec_event"])
        assertTrue(assertIs<NormalizedLoginStringArray>(normalized.fields["options"]).value.contains("adult_compliant"))
        assertFalse(body.content.contains("<llsd>"))
        assertFalse("platform_version" in normalized.fields)
        assertFalse("platform_string" in normalized.fields)
        assertFalse(LoginKeys.HOST_ID in normalized.fields)
        assertFalse(LoginKeys.TOKEN in normalized.fields)
        assertFalse(LoginKeys.EXTENDED_ERRORS in normalized.fields)
        assertFalse("max-agent-groups" in assertIs<NormalizedLoginStringArray>(normalized.fields["options"]).value)
        assertTrue(LoginKeys.SECRET in request.redactionKeys)
        assertTrue(LoginKeys.MAC in request.redactionKeys)
        assertTrue(LoginKeys.ID0 in request.redactionKeys)
        assertFalse(LoginKeys.HOST_ID in request.redactionKeys)
        assertFalse(LoginKeys.TOKEN in request.redactionKeys)
        assertFalse(body.content.contains("proof-handle"))
    }

    @Test
    fun `login activates session from xml rpc response`() {
        val clientSession = LibomvClientSession.inactive()
        val runtime = ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = RecordingHttpClient(xmlRpcSuccessBody("live-session")),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val session = assertIs<SessionLoginResult.Success>(
            runtime.login(loginRequest("proof-handle")),
        ).session
        val identity = assertIs<LibomvSessionIdentityResult.Success>(clientSession.requireIdentity(session)).identity

        assertEquals("live-session", session.sessionId.value)
        assertEquals("agent-id", identity.agentId)
        assertEquals(secureUrl("caps.example", "/private"), identity.seedCapability)
        assertEquals("203.0.113.8", identity.simulatorIp)
        assertEquals(13000, identity.simulatorPort)
        assertEquals((1024L shl 32) or 2048L, identity.regionHandle)
        assertEquals(123456789L, identity.circuitCode)
    }

    @Test
    fun `login fails closed when viewer identity cannot resolve`() {
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
            viewerIdentityProvider = HostessViewerIdentityProvider { throw IllegalStateException("host identity unavailable") },
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("viewer identity unavailable", result.failure.redactedMessage)
        assertFalse(httpClient.called)
        assertFalse(result.failure.redactedMessage.orEmpty().contains("host identity unavailable"))
    }

    @Test
    fun `login fails closed when machine identity cannot resolve`() {
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = HostessMachineIdentityProvider { throw IllegalStateException("host identity unavailable") },
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("viewer identity unavailable", result.failure.redactedMessage)
        assertFalse(httpClient.called)
        assertFalse(result.failure.redactedMessage.orEmpty().contains("host identity unavailable"))
    }

    @Test
    fun `login fails closed when shared secret cannot become login package`() {
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret(sharedSecret = "\$1\$not-md5") },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login secret invalid", result.failure.redactedMessage)
        assertFalse(httpClient.called)
    }

    @Test
    fun `login maps transport failure to redacted failure`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = ThrowingHttpClient(),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login transport failed: redacted transport failure", result.failure.redactedMessage)
    }

    @Test
    fun `login includes redacted http response detail on non success status`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = RecordingHttpClient(
                body = "Forbidden token=secret https://login.example.invalid/raw".encodeToByteArray(),
                statusCode = 403,
            ),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals(
            "login transport failed: http_status=403; response=Forbidden token=[redacted] [redacted-url]",
            result.failure.redactedMessage,
        )
        assertFalse(result.failure.redactedMessage.orEmpty().contains("secret"))
        assertFalse(result.failure.redactedMessage.orEmpty().contains("login.example.invalid"))
    }

    @Test
    fun `login maps malformed response to redacted failure`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = RecordingHttpClient("<llsd><map>".encodeToByteArray()),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login response malformed: response=<llsd><map>", result.failure.redactedMessage)
    }

    @Test
    fun `login blocks challenge response without leaking raw server text`() {
        val runtime = ProtocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = RecordingHttpClient(failureBody("Terms of Service requires agree_to_tos")),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        assertIs<SessionLoginResult.Failure>(result)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals(
            "login blocked: terms of service required: " +
                "message=Terms of Service requires agree_to_tos; login=false",
            result.failure.redactedMessage,
        )
    }

    @Test
    fun `login can pass while later identity fails without simulator fields`() {
        val clientSession = LibomvClientSession.inactive()
        val runtime = ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = RecordingHttpClient(successBody("live-session", includeSimulatorFields = false)),
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = machineIdentityProvider(),
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
        val runtime = ProtocolLoginRuntime(clientSession, RecordingHttpClient(), viewerIdentityProvider())

        val result = runtime.logout(session)

        assertEquals(SessionLogoutResult.LoggedOut, result)
        assertEquals("protocol session inactive", clientSession.requireSession(session)?.redactedMessage)
    }

    @Test
    fun `logout rejects mismatched session without leaking IDs`() {
        val clientSession = LibomvClientSession.active(hostessSession("live-session"))
        val runtime = ProtocolLoginRuntime(clientSession, RecordingHttpClient(), viewerIdentityProvider())

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

    private fun resolvedSecret(sharedSecret: String = "resolved-secret"): LoginSecret = LoginSecret(
        loginUri = loginUrl(),
        firstName = "Venue",
        lastName = "Host",
        sharedSecret = sharedSecret,
    )

    private fun viewerIdentityProvider(): HostessViewerIdentityProvider = HostessViewerIdentityProvider {
        HostessViewerIdentity(
            channel = "Hostess",
            version = "0.1.0.0",
            author = "Hostess",
            platform = HostessPlatformIdentity(
                platform = "Linux",
                platformVersion = "6.8.0",
                platformString = "Linux 6.8.0 amd64 Test Runtime 17",
            ),
            host = HostessHostIdentity(
                mac = "00000000000000000000000000000001",
                id0 = "00000000000000000000000000000002",
                hostId = "00000000000000000000000000000003",
            ),
        )
    }

    private fun machineIdentityProvider(): HostessMachineIdentityProvider = HostessMachineIdentityProvider {
        HostessMachineIdentity(
            mac = "08:00:27:DC:4A:9E",
            id0 = "08:00:27:DC:4A:9E",
        )
    }

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

    private fun xmlRpcSuccessBody(sessionValue: String): ByteArray = buildString {
        append("<methodResponse><params><param><value><struct>")
        xmlRpcMember(LoginKeys.LOGIN, xmlRpcString("true"))
        xmlRpcMember(LoginKeys.AGENT_ID, xmlRpcString("agent-id"))
        xmlRpcMember(LoginKeys.SESSION_ID, xmlRpcString(sessionValue))
        xmlRpcMember(LoginKeys.SEED_CAPABILITY, xmlRpcString(secureUrl("caps.example", "/private")))
        xmlRpcMember(LoginKeys.SIM_IP, xmlRpcString("203.0.113.8"))
        xmlRpcMember(LoginKeys.SIM_PORT, xmlRpcInt("13000"))
        xmlRpcMember(LoginKeys.REGION_X, xmlRpcInt("1024"))
        xmlRpcMember(LoginKeys.REGION_Y, xmlRpcInt("2048"))
        xmlRpcMember(LoginKeys.CIRCUIT_CODE, xmlRpcInt("123456789"))
        append("</struct></value></param></params></methodResponse>")
    }.encodeToByteArray()

    private fun failureBody(message: String): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "false")
        field(LoginKeys.MESSAGE, message)
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(key).append("</key><string>").append(value).append("</string>")
    }

    private fun StringBuilder.integer(key: String, value: String) {
        append("<key>").append(key).append("</key><integer>").append(value).append("</integer>")
    }

    private fun StringBuilder.xmlRpcMember(key: String, value: String) {
        append("<member><name>").append(key).append("</name><value>")
        append(value)
        append("</value></member>")
    }

    private fun xmlRpcString(value: String): String = "<string>$value</string>"

    private fun xmlRpcInt(value: String): String = "<i4>$value</i4>"

    private class RecordingHttpClient(
        private val body: ByteArray = ByteArray(0),
        private val statusCode: Int = 200,
    ) : ProtocolHttpClient {
        var capturedRequest: ProtocolHttpRequest? = null
        val called: Boolean
            get() = capturedRequest != null

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            capturedRequest = request
            return ProtocolHttpResponse(
                statusCode = statusCode,
                headers = emptyMap(),
                body = body,
                redactedSummary = "POST ${secureUrl("login.example", "/<redacted>")} -> $statusCode",
            )
        }
    }

    private class ThrowingHttpClient : ProtocolHttpClient {
        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            throw ProtocolHttpException("redacted transport failure")
        }
    }

    private object FailsIfCalledViewerIdentityProvider : HostessViewerIdentityProvider {
        override fun resolve(): HostessViewerIdentity {
            error("viewer identity must not resolve before the login secret")
        }
    }

    private companion object {
        val SECOND_LIFE_HASH_PATTERN = Regex("\\$1\\$[0-9a-f]{32}")

        fun loginUrl(): String = secureUrl("login.example", "/agent")

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
