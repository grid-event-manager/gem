package org.gem.protocol.libomv

import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemDelay
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupSendState
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.SessionId
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.ports.AvatarReadinessProofStatus
import org.gem.core.ports.AvatarReadinessResult
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.SessionLoginResult
import org.gem.protocol.libomv.mapping.LoginAppearanceState
import org.gem.protocol.libomv.mapping.LoginInventoryFolder
import org.gem.protocol.libomv.mapping.LoginKeys
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import org.gem.protocol.libomv.runtime.EnvironmentLoginSecretResolver
import org.gem.protocol.libomv.runtime.GemMachineIdentity
import org.gem.protocol.libomv.runtime.GemMachineIdentityProvider
import org.gem.protocol.libomv.runtime.GemHostIdentity
import org.gem.protocol.libomv.runtime.JvmMd5DigestPort
import org.gem.protocol.libomv.runtime.LibomvPlatformAdapterBundle
import org.gem.protocol.libomv.runtime.LoginSecret
import org.gem.protocol.libomv.runtime.LoginSecretResolver
import org.gem.protocol.libomv.runtime.Md5DigestPort
import org.gem.protocol.libomv.runtime.GemPlatformIdentity
import org.gem.protocol.libomv.runtime.GemViewerIdentity
import org.gem.protocol.libomv.runtime.GemViewerIdentityProvider
import org.gem.protocol.libomv.transport.ProtocolHttpBody
import org.gem.protocol.libomv.transport.ProtocolHttpClient
import org.gem.protocol.libomv.transport.ProtocolHttpRequest
import org.gem.protocol.libomv.transport.ProtocolHttpResponse
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.RegionProtocolFlags
import org.gem.protocol.libomv.transport.RecordingSimulatorSessionGateway
import org.gem.protocol.libomv.transport.SimulatorCircuitSendResult
import org.gem.protocol.libomv.transport.SimulatorPresenceResult
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
        assertIs<LibomvAvatarAdapter>(runtime.avatarPort)
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
            platformBundle(
                httpClient = httpClient,
                secretResolver = resolver,
                viewerIdentityProvider = viewerIdentityProvider(),
                machineIdentityProvider = machineIdentityProvider(),
            ),
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
        assertEquals(GemDelay.ofSeconds(120), request?.timeout)
        val body = assertIs<ProtocolHttpBody.TextBody>(request?.body).content
        assertTrue(body.contains("<methodName>login_to_simulator</methodName>"))
        assertTrue(body.contains("<name>${LoginKeys.SECRET}</name>"))
        assertTrue(body.contains("<name>first</name><value><string>Venue</string></value>"))
        assertTrue(body.contains("<name>${LoginKeys.CHANNEL}</name><value><string>GEM</string></value>"))
        assertTrue(body.contains("<name>${LoginKeys.MAC}</name><value><string>08:00:27:DC:4A:9E</string></value>"))
        assertFalse(body.contains("<llsd>"))
        assertFalse(body.contains(LoginKeys.HOST_ID))
        assertFalse(body.contains("HOSTESS_SL_SECRET"))
    }

    @Test
    fun `live runtime start location probe uses shared login resolver`() {
        val resolver = EnvironmentLoginSecretResolver { name ->
            when (name) {
                "HOSTESS_SL_SECRET" -> envSecretJson(startLocation = "uri:London City&76&174&23")
                else -> null
            }
        }
        val runtime = ProtocolLibomvModule.liveRuntime(platformBundle(secretResolver = resolver))

        val startLocation = runtime.loginStartLocationProbe.startLocation(CredentialHandle("HOSTESS_SL_SECRET"))

        assertEquals("uri:London City&76&174&23", startLocation)
    }

    @Test
    fun `public live runtime overload uses supplied login resolver`() {
        val resolver = LoginSecretResolver { handle ->
            if (handle.value == "gem-vault:v1:venue") {
                LoginSecret(
                    loginUri = secureUrl("login.example", "/cgi-bin/login.cgi"),
                    firstName = "Venue",
                    lastName = "Host",
                    sharedSecret = "secret12",
                    startLocation = "uri:London City&76&174&23",
                )
            } else {
                null
            }
        }
        val runtime = ProtocolLibomvModule.liveRuntime(resolver)

        val startLocation = runtime.loginStartLocationProbe.startLocation(
            CredentialHandle("gem-vault:v1:venue"),
        )

        assertEquals("uri:London City&76&174&23", startLocation)
    }

    @Test
    fun `live runtime notice adapter reaches protocol runtime source`() {
        val gateway = RecordingSimulatorSessionGateway(
            noticeResult = SimulatorCircuitSendResult.Sent("transportAck=passed"),
        )
        val runtime = ProtocolLibomvModule.liveRuntime(
            platformBundle(
                circuitSender = ProtocolSimulatorCircuitClient(gateway),
            ),
        )
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

        assertEquals(GroupSendState.SENT, status.state)
        assertEquals(1, gateway.noticePackets.size)
        assertEquals("33333333-3333-3333-3333-333333333333", gateway.noticePackets.single().targetGroupId)
    }

    @Test
    fun `live runtime group adapter reaches current groups source`() {
        val httpClient = RecordingHttpClient(body = ByteArray(0), statusCode = 503)
        val runtime = ProtocolLibomvModule.liveRuntime(platformBundle(httpClient = httpClient))
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
        assertContains(groups.failure.redactedMessage.orEmpty(), "capability seed unavailable")
        assertContains(groups.failure.redactedMessage.orEmpty(), "http_status=503")
        assertEquals(secureUrl("caps.example", "/seed"), httpClient.capturedRequest?.url)
    }

    @Test
    fun `live runtime inventory adapter reaches inventory catalogue source`() {
        val httpClient = SequencedHttpClient(
            listOf(
                seedCapabilitiesBody().encodeToByteArray(),
                inventoryFolderBody().encodeToByteArray(),
            ),
        )
        val runtime = ProtocolLibomvModule.liveRuntime(platformBundle(httpClient = httpClient))
        val session = fakeActiveUuidSession()
        runtime.clientSession.activate(
            session = session,
            agentId = "11111111-1111-1111-1111-111111111111",
            seedCapability = secureUrl("caps.example", "/seed"),
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
            inventoryRoots = LoginInventoryRoots(
                inventoryRootId = "44444444-4444-4444-4444-444444444444",
                inventorySkeleton = emptyList(),
                libraryRootId = null,
                libraryOwnerId = null,
                librarySkeleton = emptyList(),
            ),
        )

        val result = assertIs<InventoryItemListResult.Success>(
            runtime.inventoryPort.listItems(session, InventoryItemQuery()),
        )

        assertEquals(listOf("Venue Landmark"), result.items.map { it.displayName.value })
        assertEquals(2, httpClient.requests.size)
        assertEquals(secureUrl("caps.example", "/seed"), httpClient.requests[0].url)
        assertEquals(secureUrl("caps.example", "/fetch-descendents"), httpClient.requests[1].url)
    }

    @Test
    fun `live runtime avatar adapter reaches protocol avatar runtime source`() {
        val httpClient = SequencedHttpClient(
            listOf(
                seedCapabilitiesBody().encodeToByteArray(),
                avatarAppearanceSuccessBody().encodeToByteArray(),
            ),
        )
        val gateway = RecordingSimulatorSessionGateway(
            presenceResult = SimulatorPresenceResult.Present(
                pingReplies = 0,
                cached = false,
                regionProtocolFlags = RegionProtocolFlags(agentAppearanceService = true),
            ),
        )
        val runtime = ProtocolLibomvModule.liveRuntime(
            platformBundle(
                httpClient = httpClient,
                circuitSender = ProtocolSimulatorCircuitClient(gateway),
            ),
        )
        val session = fakeActiveUuidSession()
        runtime.clientSession.activate(
            session = session,
            agentId = "11111111-1111-1111-1111-111111111111",
            seedCapability = secureUrl("caps.example", "/seed"),
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
            inventoryRoots = LoginInventoryRoots(
                inventoryRootId = "44444444-4444-4444-4444-444444444444",
                inventorySkeleton = listOf(
                    LoginInventoryFolder(
                        folderId = "55555555-5555-5555-5555-555555555555",
                        parentId = "44444444-4444-4444-4444-444444444444",
                        ownerId = "11111111-1111-1111-1111-111111111111",
                        name = "Current Outfit",
                        typeDefault = 46,
                        version = 17,
                    ),
                ),
                libraryRootId = null,
                libraryOwnerId = null,
                librarySkeleton = emptyList(),
            ),
            appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 19),
        )

        val result = assertIs<AvatarReadinessResult.Success>(runtime.avatarPort.ensureReady(session))

        assertEquals(AvatarReadinessProofStatus.PASSED, result.proof.serverAppearanceStatus)
        assertEquals(2, httpClient.requests.size)
        assertEquals(secureUrl("caps.example", "/seed"), httpClient.requests[0].url)
        assertEquals(secureUrl("caps.example", "/appearance"), httpClient.requests[1].url)
        val body = assertIs<ProtocolHttpBody.TextBody>(httpClient.requests[1].body).content
        assertContains(body, "<key>cof_version</key><integer>17</integer>")
        assertFalse(body.contains("11111111-1111-1111-1111-111111111111"))
    }

    @Test
    fun `injected bundle booleans drive load state and fail closed`() {
        val runtimeBlocked = ProtocolLibomvModule.liveRuntime(
            platformBundle(
                runtimeLoad = false,
                transportLoad = false,
            ),
        )

        assertTrue(runtimeBlocked.loadState.adapterLoad)
        assertFalse(runtimeBlocked.loadState.runtimeLoad)
        assertFalse(runtimeBlocked.loadState.transportLoad)
        val login = assertIs<SessionLoginResult.Failure>(runtimeBlocked.sessionPort.login(loginRequest()))
        assertEquals("protocol runtime unavailable", login.failure.redactedMessage)
        val avatarBlocked = assertIs<AvatarReadinessResult.Failure>(
            runtimeBlocked.avatarPort.ensureReady(fakeActiveUuidSession()),
        )
        assertEquals(AvatarReadinessProofStatus.RUNTIME_GAP, avatarBlocked.proof.avatarReadinessStatus)
        assertEquals(AvatarReadinessProofStatus.NOT_RUN, avatarBlocked.proof.simulatorPresenceStatus)

        val transportBlocked = ProtocolLibomvModule.liveRuntime(platformBundle(transportLoad = false))
        val session = fakeActiveUuidSession()
        transportBlocked.clientSession.activate(
            session = session,
            agentId = "11111111-1111-1111-1111-111111111111",
            seedCapability = secureUrl("caps.example", "/seed"),
            simulatorIp = "203.0.113.8",
            simulatorPort = 13000,
            regionHandle = 123456789L,
            circuitCode = 987654321L,
        )

        val groups = assertIs<GroupListResult.Failure>(transportBlocked.groupPort.currentGroups(session))

        assertTrue(transportBlocked.loadState.runtimeLoad)
        assertFalse(transportBlocked.loadState.transportLoad)
        assertEquals("current groups unavailable", groups.failure.redactedMessage)
        val avatarTransportBlocked = assertIs<AvatarReadinessResult.Failure>(
            transportBlocked.avatarPort.ensureReady(session),
        )
        assertEquals(AvatarReadinessProofStatus.RUNTIME_GAP, avatarTransportBlocked.proof.avatarReadinessStatus)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("venue-proof"),
        credentialHandle = CredentialHandle("proof-handle"),
    )

    private fun fakeInactiveSession(): GemSession = GemSession(
        sessionId = SessionId("inactive-proof-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = false,
    )

    private fun fakeActiveUuidSession(): GemSession = GemSession(
        sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
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

    private class SequencedHttpClient(
        private val bodies: List<ByteArray>,
    ) : ProtocolHttpClient {
        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            val index = (requests.size - 1).coerceAtMost(bodies.lastIndex)
            return ProtocolHttpResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = bodies[index],
                redactedSummary = "POST <redacted> -> 200",
            )
        }
    }

    private fun envSecretJson(startLocation: String = "last"): String = """
        {
          "loginUri": "${secureUrl("login.example", "/cgi-bin/login.cgi")}",
          "firstName": "Venue",
          "lastName": "Host",
          "sharedSecret": "secret12",
          "startLocation": "$startLocation"
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

    private fun seedCapabilitiesBody(): String = """
        <llsd>
          <map>
            <key>EventQueueGet</key><uri>${secureUrl("caps.example", "/event")}</uri>
            <key>FetchInventoryDescendents2</key><uri>${secureUrl("caps.example", "/fetch-descendents")}</uri>
            <key>UpdateAvatarAppearance</key><uri>${secureUrl("caps.example", "/appearance")}</uri>
          </map>
        </llsd>
    """.trimIndent()

    private fun avatarAppearanceSuccessBody(): String = """
        <llsd>
          <map>
            <key>success</key><boolean>true</boolean>
          </map>
        </llsd>
    """.trimIndent()

    private fun inventoryFolderBody(): String = """
        <llsd>
          <map>
            <key>folders</key>
            <array>
              <map>
                <key>categories</key><array />
                <key>items</key>
                <array>
                  <map>
                    <key>item_id</key><uuid>55555555-5555-5555-5555-555555555555</uuid>
                    <key>agent_id</key><uuid>11111111-1111-1111-1111-111111111111</uuid>
                    <key>parent_id</key><uuid>44444444-4444-4444-4444-444444444444</uuid>
                    <key>asset_id</key><uuid>66666666-6666-6666-6666-666666666666</uuid>
                    <key>name</key><string>Venue Landmark</string>
                    <key>inv_type</key><integer>3</integer>
                  </map>
                </array>
              </map>
            </array>
          </map>
        </llsd>
    """.trimIndent()

    private fun viewerIdentityProvider(): GemViewerIdentityProvider = GemViewerIdentityProvider {
        GemViewerIdentity(
            channel = "GEM",
            version = "0.1.0.0",
            author = "GEM",
            platform = GemPlatformIdentity(
                platform = "Linux",
                platformVersion = "6.8.0",
                platformString = "Linux 6.8.0 amd64 Test Runtime 17",
            ),
            host = GemHostIdentity(
                mac = "00000000000000000000000000000001",
                id0 = "00000000000000000000000000000002",
                hostId = "00000000000000000000000000000003",
            ),
        )
    }

    private fun machineIdentityProvider(): GemMachineIdentityProvider = GemMachineIdentityProvider {
        GemMachineIdentity(
            mac = "08:00:27:DC:4A:9E",
            id0 = "08:00:27:DC:4A:9E",
        )
    }

    private fun platformBundle(
        httpClient: ProtocolHttpClient = RecordingHttpClient(ByteArray(0)),
        secretResolver: LoginSecretResolver = LoginSecretResolver.unavailable(),
        viewerIdentityProvider: GemViewerIdentityProvider = viewerIdentityProvider(),
        machineIdentityProvider: GemMachineIdentityProvider = machineIdentityProvider(),
        clockPort: ClockPort = FixedClockPort,
        md5DigestPort: Md5DigestPort = JvmMd5DigestPort,
        circuitSender: ProtocolSimulatorCircuitClient = ProtocolSimulatorCircuitClient(
            RecordingSimulatorSessionGateway(),
        ),
        adapterLoad: Boolean = true,
        runtimeLoad: Boolean = true,
        transportLoad: Boolean = true,
    ): LibomvPlatformAdapterBundle =
        LibomvPlatformAdapterBundle(
            httpClient = httpClient,
            secretResolver = secretResolver,
            viewerIdentityProvider = viewerIdentityProvider,
            machineIdentityProvider = machineIdentityProvider,
            clockPort = clockPort,
            md5DigestPort = md5DigestPort,
            circuitSender = circuitSender,
            adapterLoad = adapterLoad,
            runtimeLoad = runtimeLoad,
            transportLoad = transportLoad,
        )

    private object FixedClockPort : ClockPort {
        override fun now(): GemInstant = GemInstant.EPOCH

        override fun pause(duration: GemDelay) = Unit
    }

    private companion object {
        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
