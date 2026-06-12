package org.gem.protocol.libomv.runtime

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
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentityResult
import org.gem.protocol.libomv.mapping.LoginAppearanceState
import org.gem.protocol.libomv.mapping.LoginAppearanceStateResult
import org.gem.protocol.libomv.mapping.LoginInventoryRootsResult
import org.gem.protocol.libomv.mapping.LoginKeys
import org.gem.protocol.libomv.transport.ClosedWithoutReply
import org.gem.protocol.libomv.transport.Failed
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.ProtocolHttpBody
import org.gem.protocol.libomv.transport.ProtocolHttpClient
import org.gem.protocol.libomv.transport.ProtocolHttpException
import org.gem.protocol.libomv.transport.ProtocolHttpRequest
import org.gem.protocol.libomv.transport.ProtocolHttpResponse
import org.gem.protocol.libomv.transport.RecordingSimulatorSessionGateway
import org.gem.protocol.libomv.transport.toSimulatorCircuit
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
            clientSession = clientSession,
            httpClient = httpClient,
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            clockPort = FixedClockPort(GemInstant(1_780_603_200_000L)),
            machineIdentityProvider = machineIdentityProvider(),
        )

        val result = runtime.login(loginRequest("proof-handle"))

        val session = assertIs<SessionLoginResult.Success>(result).session
        assertEquals("live-session", session.sessionId.value)
        assertEquals("venue-proof", session.accountLabel.value)
        assertEquals(GemInstant(1_780_603_200_000L), session.startedAt)
        assertTrue(session.isActive)
        assertNull(clientSession.requireSession(session))
        val identity = assertIs<LibomvSessionIdentityResult.Success>(clientSession.requireIdentity(session)).identity
        assertEquals("agent-id", identity.agentId)
        assertEquals(secureUrl("caps.example", "/private"), identity.seedCapability)
        assertEquals("203.0.113.8", identity.simulatorIp)
        assertEquals(13000, identity.simulatorPort)
        assertEquals((1024L shl 32) or 2048L, identity.regionHandle)
        assertEquals(123456789L, identity.circuitCode)
        assertEquals("Venue Host", identity.agentName)
        val roots = assertIs<LoginInventoryRootsResult.Success>(clientSession.inventoryRoots(session)).roots
        assertEquals("inventory-root-id", roots.inventoryRootId)
        assertEquals("landmarks-folder-id", roots.inventorySkeleton.single().folderId)
        val appearanceState = assertIs<LoginAppearanceStateResult.Success>(
            clientSession.appearanceState(session),
        ).appearanceState
        assertEquals(LoginAppearanceState(agentAppearanceService = true, cofVersion = 47), appearanceState)

        val request = httpClient.capturedRequest ?: error("login request was not captured")
        assertEquals("POST", request.method)
        assertEquals(loginUrl(), request.url)
        assertEquals("text/xml", request.headers["Content-Type"])
        assertEquals(GemDelay.ofSeconds(120), request.timeout)
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body)
        assertEquals("text/xml", body.contentType)
        val normalized = LoginPackageCaptureNormalizer.normalize(body.content)
        assertEquals("login_to_simulator", normalized.methodName)
        assertEquals(NormalizedLoginString("string", "Venue"), normalized.fields["first"])
        assertEquals(NormalizedLoginString("string", "Host"), normalized.fields["last"])
        assertEquals(NormalizedLoginString("string", "last"), normalized.fields["start"])
        assertEquals(NormalizedLoginString("string", "GEM"), normalized.fields[LoginKeys.CHANNEL])
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
        val runtime = protocolLoginRuntime(
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
        assertEquals("Venue Host", identity.agentName)
        val roots = assertIs<LoginInventoryRootsResult.Success>(clientSession.inventoryRoots(session)).roots
        assertEquals("xml-inventory-root-id", roots.inventoryRootId)
        assertEquals("xml-landmarks-folder-id", roots.inventorySkeleton.single().folderId)
        assertEquals(
            LoginAppearanceState(agentAppearanceService = false, cofVersion = 48),
            assertIs<LoginAppearanceStateResult.Success>(clientSession.appearanceState(session)).appearanceState,
        )
    }

    @Test
    fun `login fails closed when viewer identity cannot resolve`() {
        val httpClient = RecordingHttpClient(successBody("live-session"))
        val runtime = protocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
            viewerIdentityProvider = GemViewerIdentityProvider { throw IllegalStateException("host identity unavailable") },
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
        val runtime = protocolLoginRuntime(
            clientSession = LibomvClientSession.inactive(),
            httpClient = httpClient,
            viewerIdentityProvider = viewerIdentityProvider(),
            secretResolver = LoginSecretResolver { resolvedSecret() },
            machineIdentityProvider = GemMachineIdentityProvider { throw IllegalStateException("host identity unavailable") },
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
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
        val runtime = protocolLoginRuntime(
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
    fun `logout calls simulator before clearing matching session`() {
        val session = logoutSession()
        val clientSession = activeClientSession(session)
        val gateway = RecordingSimulatorSessionGateway()
        val runtime = protocolLoginRuntime(
            clientSession,
            RecordingHttpClient(),
            viewerIdentityProvider(),
            circuitClient = ProtocolSimulatorCircuitClient(gateway),
        )

        val result = runtime.logout(session)

        assertEquals(SessionLogoutResult.LoggedOut, result)
        assertEquals("protocol session inactive", clientSession.requireSession(session)?.redactedMessage)
        assertEquals(session.toIdentityCircuit(), gateway.logoutCircuits.single())
    }

    @Test
    fun `logout clears matching session after bounded close without reply`() {
        val session = logoutSession()
        val clientSession = activeClientSession(session)
        val gateway = RecordingSimulatorSessionGateway(logoutResult = ClosedWithoutReply)
        val runtime = protocolLoginRuntime(
            clientSession,
            RecordingHttpClient(),
            viewerIdentityProvider(),
            circuitClient = ProtocolSimulatorCircuitClient(gateway),
        )

        val result = runtime.logout(session)

        assertEquals(SessionLogoutResult.LoggedOut, result)
        assertEquals("protocol session inactive", clientSession.requireSession(session)?.redactedMessage)
        assertEquals(session.toIdentityCircuit(), gateway.logoutCircuits.single())
    }

    @Test
    fun `logout failure does not clear matching session`() {
        val session = logoutSession()
        val clientSession = activeClientSession(session)
        val gateway = RecordingSimulatorSessionGateway(
            logoutResult = Failed("logout transport failed"),
        )
        val runtime = protocolLoginRuntime(
            clientSession,
            RecordingHttpClient(),
            viewerIdentityProvider(),
            circuitClient = ProtocolSimulatorCircuitClient(gateway),
        )

        val result = assertIs<SessionLogoutResult.Failure>(runtime.logout(session))

        assertEquals(CoreFailureReason.LOGOUT_FAILED, result.failure.reason)
        assertEquals("logout transport failed", result.failure.redactedMessage)
        assertNull(clientSession.requireSession(session))
        assertEquals(session.toIdentityCircuit(), gateway.logoutCircuits.single())
    }

    @Test
    fun `logout rejects mismatched session without leaking IDs`() {
        val activeSession = logoutSession()
        val mismatchedSession = gemSession("other-session")
        val clientSession = activeClientSession(activeSession)
        val gateway = RecordingSimulatorSessionGateway()
        val runtime = protocolLoginRuntime(
            clientSession,
            RecordingHttpClient(),
            viewerIdentityProvider(),
            circuitClient = ProtocolSimulatorCircuitClient(gateway),
        )

        val result = runtime.logout(mismatchedSession)

        val failure = assertIs<SessionLogoutResult.Failure>(result).failure
        assertEquals(CoreFailureReason.LOGOUT_FAILED, failure.reason)
        assertEquals("gem session mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains(activeSession.sessionId.value))
        assertFalse(failure.redactedMessage.orEmpty().contains("other-session"))
        assertNull(clientSession.requireSession(activeSession))
        assertTrue(gateway.logoutCircuits.isEmpty())
    }

    @Test
    fun `invalid simulator circuit blocks logout without clearing local session`() {
        val session = logoutSession()
        val clientSession = activeClientSession(session, agentId = "agent-id")
        val gateway = RecordingSimulatorSessionGateway()
        val runtime = protocolLoginRuntime(
            clientSession,
            RecordingHttpClient(),
            viewerIdentityProvider(),
            circuitClient = ProtocolSimulatorCircuitClient(gateway),
        )

        val result = assertIs<SessionLogoutResult.Failure>(runtime.logout(session))

        assertEquals(CoreFailureReason.LOGOUT_FAILED, result.failure.reason)
        assertEquals("protocol simulator send failed", result.failure.redactedMessage)
        assertNull(clientSession.requireSession(session))
        assertTrue(gateway.logoutCircuits.isEmpty())
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

    private fun gemSession(id: String): GemSession = GemSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun logoutSession(): GemSession = gemSession(SESSION_ID)

    private fun activeClientSession(
        session: GemSession,
        agentId: String = AGENT_ID,
    ): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = agentId,
        seedCapability = secureUrl("caps.example", "/private"),
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = (1024L shl 32) or 2048L,
        circuitCode = 123456789L,
    )

    private fun GemSession.toIdentityCircuit() =
        assertIs<LibomvSessionIdentityResult.Success>(
            activeClientSession(this).requireIdentity(this),
        ).identity.toSimulatorCircuit()

    private fun protocolLoginRuntime(
        clientSession: LibomvClientSession,
        httpClient: ProtocolHttpClient,
        viewerIdentityProvider: GemViewerIdentityProvider,
        secretResolver: LoginSecretResolver = LoginSecretResolver.unavailable(),
        clockPort: ClockPort = FixedClockPort(GemInstant.EPOCH),
        machineIdentityProvider: GemMachineIdentityProvider = machineIdentityProvider(),
        digestPort: Md5DigestPort = JvmMd5DigestPort,
        circuitClient: ProtocolSimulatorCircuitClient = ProtocolSimulatorCircuitClient(
            RecordingSimulatorSessionGateway(),
        ),
    ): ProtocolLoginRuntime =
        ProtocolLoginRuntime(
            clientSession = clientSession,
            httpClient = httpClient,
            circuitClient = circuitClient,
            viewerIdentityProvider = viewerIdentityProvider,
            secretResolver = secretResolver,
            clockPort = clockPort,
            machineIdentityProvider = machineIdentityProvider,
            digestPort = digestPort,
        )

    private class FixedClockPort(
        private val now: GemInstant,
    ) : ClockPort {
        override fun now(): GemInstant = now

        override fun pause(duration: GemDelay) = Unit
    }

    private fun successBody(
        sessionValue: String,
        includeSimulatorFields: Boolean = true,
    ): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "true")
        field(LoginKeys.AGENT_ID, "agent-id")
        field(LoginKeys.SESSION_ID, sessionValue)
        field(LoginKeys.AGENT_APPEARANCE_SERVICE, "yes")
        field(LoginKeys.COF_VERSION, "47")
        field(LoginKeys.SEED_CAPABILITY, secureUrl("caps.example", "/private"))
        if (includeSimulatorFields) {
            field(LoginKeys.SIM_IP, "203.0.113.8")
            integer(LoginKeys.SIM_PORT, "13000")
            integer(LoginKeys.REGION_X, "1024")
            integer(LoginKeys.REGION_Y, "2048")
            integer(LoginKeys.CIRCUIT_CODE, "123456789")
        }
        mappedUuid(LoginKeys.INVENTORY_ROOT, LoginKeys.FOLDER_ID, "inventory-root-id")
        append("<key>").append(LoginKeys.INVENTORY_SKELETON).append("</key><array>")
        folder("landmarks-folder-id", "inventory-root-id", "Landmarks", 3, 42)
        append("</array>")
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun xmlRpcSuccessBody(sessionValue: String): ByteArray = buildString {
        append("<methodResponse><params><param><value><struct>")
        xmlRpcMember(LoginKeys.LOGIN, xmlRpcString("true"))
        xmlRpcMember(LoginKeys.AGENT_ID, xmlRpcString("agent-id"))
        xmlRpcMember(LoginKeys.SESSION_ID, xmlRpcString(sessionValue))
        xmlRpcMember(LoginKeys.AGENT_APPEARANCE_SERVICE, xmlRpcString("no"))
        xmlRpcMember(LoginKeys.COF_VERSION, xmlRpcInt("48"))
        xmlRpcMember(LoginKeys.SEED_CAPABILITY, xmlRpcString(secureUrl("caps.example", "/private")))
        xmlRpcMember(LoginKeys.SIM_IP, xmlRpcString("203.0.113.8"))
        xmlRpcMember(LoginKeys.SIM_PORT, xmlRpcInt("13000"))
        xmlRpcMember(LoginKeys.REGION_X, xmlRpcInt("1024"))
        xmlRpcMember(LoginKeys.REGION_Y, xmlRpcInt("2048"))
        xmlRpcMember(LoginKeys.CIRCUIT_CODE, xmlRpcInt("123456789"))
        xmlRpcMember(LoginKeys.INVENTORY_ROOT, xmlRpcMappedUuid(LoginKeys.FOLDER_ID, "xml-inventory-root-id"))
        xmlRpcMember(
            LoginKeys.INVENTORY_SKELETON,
            xmlRpcFolderArray(xmlRpcFolder("xml-landmarks-folder-id", "xml-inventory-root-id", "Landmarks", 3, 42)),
        )
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

    private fun StringBuilder.mappedUuid(key: String, fieldName: String, value: String) {
        append("<key>").append(key).append("</key><array><map>")
        field(fieldName, value)
        append("</map></array>")
    }

    private fun StringBuilder.folder(
        folderId: String,
        parentId: String,
        name: String,
        typeDefault: Int,
        version: Int,
    ) {
        append("<map>")
        field(LoginKeys.FOLDER_ID, folderId)
        field(LoginKeys.PARENT_ID, parentId)
        field(LoginKeys.NAME, name)
        append("<key>").append(LoginKeys.TYPE_DEFAULT).append("</key><integer>").append(typeDefault).append("</integer>")
        append("<key>").append(LoginKeys.VERSION_FIELD).append("</key><integer>").append(version).append("</integer>")
        append("</map>")
    }

    private fun StringBuilder.xmlRpcMember(key: String, value: String) {
        append("<member><name>").append(key).append("</name><value>")
        append(value)
        append("</value></member>")
    }

    private fun xmlRpcString(value: String): String = "<string>$value</string>"

    private fun xmlRpcInt(value: String): String = "<i4>$value</i4>"

    private fun xmlRpcMappedUuid(fieldName: String, value: String): String =
        xmlRpcArray(xmlRpcStruct(fieldName to xmlRpcString(value)))

    private fun xmlRpcFolderArray(vararg folders: String): String =
        xmlRpcArray(*folders)

    private fun xmlRpcFolder(
        folderId: String,
        parentId: String,
        name: String,
        typeDefault: Int,
        version: Int,
    ): String = xmlRpcStruct(
        LoginKeys.FOLDER_ID to xmlRpcString(folderId),
        LoginKeys.PARENT_ID to xmlRpcString(parentId),
        LoginKeys.NAME to xmlRpcString(name),
        LoginKeys.TYPE_DEFAULT to xmlRpcInt(typeDefault.toString()),
        LoginKeys.VERSION_FIELD to xmlRpcInt(version.toString()),
    )

    private fun xmlRpcStruct(vararg members: Pair<String, String>): String = buildString {
        append("<struct>")
        members.forEach { (name, value) ->
            append("<member><name>").append(name).append("</name><value>")
            append(value)
            append("</value></member>")
        }
        append("</struct>")
    }

    private fun xmlRpcArray(vararg values: String): String = buildString {
        append("<array><data>")
        values.forEach { value ->
            append("<value>").append(value).append("</value>")
        }
        append("</data></array>")
    }

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

    private object FailsIfCalledViewerIdentityProvider : GemViewerIdentityProvider {
        override fun resolve(): GemViewerIdentity {
            error("viewer identity must not resolve before the login secret")
        }
    }

    private companion object {
        val SECOND_LIFE_HASH_PATTERN = Regex("\\$1\\$[0-9a-f]{32}")
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"

        fun loginUrl(): String = secureUrl("login.example", "/agent")

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
