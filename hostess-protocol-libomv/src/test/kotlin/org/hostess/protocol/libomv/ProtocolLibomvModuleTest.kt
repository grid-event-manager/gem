package org.hostess.protocol.libomv

import java.time.Instant
import java.time.Duration
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.protocol.libomv.mapping.LoginKeys
import org.hostess.protocol.libomv.runtime.EnvironmentLoginSecretResolver
import org.hostess.protocol.libomv.runtime.HostessMachineIdentity
import org.hostess.protocol.libomv.runtime.HostessMachineIdentityProvider
import org.hostess.protocol.libomv.runtime.HostessHostIdentity
import org.hostess.protocol.libomv.runtime.HostessPlatformIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentityProvider
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse
import kotlin.test.Test
import kotlin.test.assertContains
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
        assertTrue(runtime.loadState.adapterLoad)
        assertTrue(runtime.loadState.runtimeLoad)
        assertTrue(runtime.loadState.transportLoad)
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
    fun `live runtime unresolved login and unimplemented methods still fail closed`() {
        val runtime = ProtocolLibomvModule.liveRuntime()

        val login = runtime.sessionPort.login(loginRequest())
        val groups = runtime.groupPort.currentGroups(fakeInactiveSession())

        assertIs<SessionLoginResult.Failure>(login)
        assertEquals(CoreFailureReason.LOGIN_FAILED, login.failure.reason)
        assertEquals("login secret unavailable", login.failure.redactedMessage)

        assertIs<GroupListResult.Failure>(groups)
        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, groups.failure.reason)
        assertEquals("protocol session inactive", groups.failure.redactedMessage)
    }

    @Test
    fun `live runtime composes env resolver inside protocol module`() {
        val httpClient = RecordingHttpClient(successBody("composed-session"))
        val resolver = EnvironmentLoginSecretResolver { name ->
            when (name) {
                "HOSTESS_SL_SECRET" -> envSecretJson()
                else -> null
            }
        }
        val runtime = ProtocolLibomvModule.liveRuntime(
            httpClient,
            resolver,
            viewerIdentityProvider(),
            machineIdentityProvider(),
        )

        val login = runtime.sessionPort.login(
            LoginRequest(
                accountLabel = AccountLabel("venue-proof"),
                credentialHandle = CredentialHandle("HOSTESS_SL_SECRET"),
            ),
        )

        val session = assertIs<SessionLoginResult.Success>(login).session
        assertEquals("composed-session", session.sessionId.value)
        val request = httpClient.capturedRequest
        assertEquals("POST", request?.method)
        assertEquals(secureUrl("login.example", "/cgi-bin/login.cgi"), request?.url)
        assertEquals("text/xml", request?.headers?.get("Content-Type"))
        assertEquals(Duration.ofSeconds(120), request?.timeout)
        val body = assertIs<ProtocolHttpBody.TextBody>(request?.body).content
        assertTrue(body.contains("<methodName>login_to_simulator</methodName>"))
        assertTrue(body.contains("<name>${LoginKeys.SECRET}</name>"))
        assertTrue(body.contains("<name>first</name><value><string>Venue</string></value>"))
        assertTrue(body.contains("<name>${LoginKeys.CHANNEL}</name><value><string>Hostess</string></value>"))
        assertTrue(body.contains("<name>${LoginKeys.MAC}</name><value><string>08:00:27:DC:4A:9E</string></value>"))
        assertFalse(body.contains("<llsd>"))
        assertFalse(body.contains(LoginKeys.HOST_ID))
        assertFalse(body.contains("HOSTESS_SL_SECRET"))
    }

    @Test
    fun `live runtime notice adapter reaches protocol runtime source`() {
        val runtime = ProtocolLibomvModule.liveRuntime()
        val session = fakeActiveUuidSession()
        runtime.clientSession.activate(
            session = session,
            agentId = "11111111-1111-1111-1111-111111111111",
            seedCapability = "seed-capability",
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
        )

        val status = runtime.noticePort.sendGroupNotice(session, group(), draft(), null)

        assertEquals(GroupSendState.FAILED, status.state)
        assertEquals("notice runtime unavailable", status.detail)
    }

    @Test
    fun `live runtime group adapter reaches current groups source`() {
        val httpClient = RecordingHttpClient(body = ByteArray(0), statusCode = 503)
        val runtime = ProtocolLibomvModule.liveRuntime(httpClient)
        val session = fakeActiveUuidSession()
        runtime.clientSession.activate(
            session = session,
            agentId = "11111111-1111-1111-1111-111111111111",
            seedCapability = secureUrl("caps.example", "/seed"),
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
        )

        val groups = assertIs<GroupListResult.Failure>(runtime.groupPort.currentGroups(session))

        assertEquals(CoreFailureReason.GROUP_LIST_FAILED, groups.failure.reason)
        assertContains(groups.failure.redactedMessage.orEmpty(), "current groups transport unavailable")
        assertContains(groups.failure.redactedMessage.orEmpty(), "http_status=503")
        assertEquals(secureUrl("caps.example", "/seed"), httpClient.capturedRequest?.url)
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

    private fun fakeActiveUuidSession(): HostessSession = HostessSession(
        sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = Instant.EPOCH,
        isActive = true,
    )

    private fun draft(): NoticeDraft = NoticeDraft(
        subject = "Gig tonight",
        message = "Doors at 8",
        targetSet = assertIs<TargetSelectionResult.Changed>(
            GroupTargetSet.from(listOf(group())).addAllSendable(),
        ).targetSet,
    )

    private fun group(): GroupMembership = GroupMembership(
        groupId = GroupId("33333333-3333-3333-3333-333333333333"),
        displayName = GroupDisplayName("Music Room"),
        canSendNotices = true,
        acceptsNotices = true,
    )

    private class RecordingHttpClient(
        private val body: ByteArray,
        private val statusCode: Int = 200,
    ) : ProtocolHttpClient {
        var capturedRequest: ProtocolHttpRequest? = null

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            capturedRequest = request
            return ProtocolHttpResponse(
                statusCode = statusCode,
                headers = emptyMap(),
                body = body,
                redactedSummary = "POST <redacted> -> $statusCode",
            )
        }
    }

    private fun envSecretJson(): String = """
        {
          "loginUri": "${secureUrl("login.example", "/cgi-bin/login.cgi")}",
          "firstName": "Venue",
          "lastName": "Host",
          "sharedSecret": "secret12",
          "startLocation": "last"
        }
    """.trimIndent()

    private fun successBody(sessionValue: String): ByteArray = """
        <llsd>
          <map>
            <key>${LoginKeys.LOGIN}</key><string>true</string>
            <key>${LoginKeys.AGENT_ID}</key><string>agent-id</string>
            <key>${LoginKeys.SESSION_ID}</key><string>$sessionValue</string>
            <key>${LoginKeys.SEED_CAPABILITY}</key><string>${secureUrl("caps.example", "/private")}</string>
          </map>
        </llsd>
    """.trimIndent().encodeToByteArray()

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

    private companion object {
        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
