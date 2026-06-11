package org.hostess.protocol.libomv

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupSendState
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.ports.AvatarReadinessProofStatus
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.protocol.libomv.mapping.LoginAppearanceState
import org.hostess.protocol.libomv.mapping.LoginInventoryFolder
import org.hostess.protocol.libomv.mapping.LoginKeys
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots
import org.hostess.protocol.libomv.runtime.EnvironmentLoginSecretResolver
import org.hostess.protocol.libomv.runtime.HostessMachineIdentity
import org.hostess.protocol.libomv.runtime.HostessMachineIdentityProvider
import org.hostess.protocol.libomv.runtime.HostessHostIdentity
import org.hostess.protocol.libomv.runtime.JvmMd5DigestPort
import org.hostess.protocol.libomv.runtime.LibomvPlatformAdapterBundle
import org.hostess.protocol.libomv.runtime.LoginSecret
import org.hostess.protocol.libomv.runtime.LoginSecretResolver
import org.hostess.protocol.libomv.runtime.Md5DigestPort
import org.hostess.protocol.libomv.runtime.HostessPlatformIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentity
import org.hostess.protocol.libomv.runtime.HostessViewerIdentityProvider
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorEndpoint
import org.hostess.protocol.libomv.transport.SimulatorInboundPacket
import org.hostess.protocol.libomv.transport.SimulatorPacketExchange
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
        assertEquals(HostessDelay.ofSeconds(120), request?.timeout)
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
            if (handle.value == "hostess-vault:v1:venue") {
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
            CredentialHandle("hostess-vault:v1:venue"),
        )

        assertEquals("uri:London City&76&174&23", startLocation)
    }

    @Test
    fun `live runtime notice adapter reaches protocol runtime source`() {
        val packetExchange = RecordingSimulatorPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete(), null, simulatorPacketAck(5)),
        )
        val runtime = ProtocolLibomvModule.liveRuntime(
            platformBundle(
                circuitSender = ProtocolSimulatorCircuitClient(packetExchange),
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
        assertEquals(6, packetExchange.payloads.size)
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
        val packetExchange = RecordingSimulatorPacketExchange(
            inboundPayloads = mutableListOf(regionHandshake(), agentMovementComplete()),
        )
        val runtime = ProtocolLibomvModule.liveRuntime(
            platformBundle(
                httpClient = httpClient,
                circuitSender = ProtocolSimulatorCircuitClient(packetExchange),
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

    private fun fakeInactiveSession(): HostessSession = HostessSession(
        sessionId = SessionId("inactive-proof-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = false,
    )

    private fun fakeActiveUuidSession(): HostessSession = HostessSession(
        sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
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

    private fun platformBundle(
        httpClient: ProtocolHttpClient = RecordingHttpClient(ByteArray(0)),
        secretResolver: LoginSecretResolver = LoginSecretResolver.unavailable(),
        viewerIdentityProvider: HostessViewerIdentityProvider = viewerIdentityProvider(),
        machineIdentityProvider: HostessMachineIdentityProvider = machineIdentityProvider(),
        clockPort: ClockPort = FixedClockPort,
        md5DigestPort: Md5DigestPort = JvmMd5DigestPort,
        circuitSender: ProtocolSimulatorCircuitClient = ProtocolSimulatorCircuitClient(NoopSimulatorPacketExchange),
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
        override fun now(): HostessInstant = HostessInstant.EPOCH

        override fun pause(duration: HostessDelay) = Unit
    }

    private object NoopSimulatorPacketExchange : SimulatorPacketExchange {
        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) = Unit

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? = null
    }

    private class RecordingSimulatorPacketExchange(
        private val inboundPayloads: MutableList<ByteArray?> = mutableListOf(),
    ) : SimulatorPacketExchange {
        var payloads: List<ByteArray> = emptyList()

        override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
            this.payloads = this.payloads + payloads
        }

        override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? =
            if (inboundPayloads.isEmpty()) {
                null
            } else {
                inboundPayloads.removeAt(0)?.let { SimulatorInboundPacket(endpoint, it) }
            }
    }

    private companion object {
        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"

        fun regionHandshake(): ByteArray =
            org.hostess.protocol.libomv.transport.LibomvPacketTestBytes.regionHandshakeWithRegionProtocols(
                regionProtocols = 1L,
            )

        fun agentMovementComplete(): ByteArray =
            org.hostess.protocol.libomv.transport.LibomvPacketTestBytes.lowHeader(
                sequence = 102,
                packetId = 250,
                flags = 0,
            )

        fun simulatorPacketAck(ackedSequence: Long): ByteArray =
            byteArrayOf(
                0,
                0,
                0,
                0,
                103,
                0,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFB.toByte(),
                1,
                (ackedSequence and 0xFF).toByte(),
                ((ackedSequence ushr 8) and 0xFF).toByte(),
                ((ackedSequence ushr 16) and 0xFF).toByte(),
                ((ackedSequence ushr 24) and 0xFF).toByte(),
            )
    }
}
