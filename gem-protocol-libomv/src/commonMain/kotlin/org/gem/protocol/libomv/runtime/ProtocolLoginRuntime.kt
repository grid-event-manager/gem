package org.gem.protocol.libomv.runtime

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemSession
import org.gem.core.ports.ClockPort
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.core.services.SafeDiagnosticRedaction
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentityResult
import org.gem.protocol.libomv.mapping.LibomvLoginFailureKind
import org.gem.protocol.libomv.mapping.LibomvLoginMapping
import org.gem.protocol.libomv.mapping.LibomvLoginMappingResult
import org.gem.protocol.libomv.mapping.LoginKeys
import org.gem.protocol.libomv.transport.ClosedWithoutReply
import org.gem.protocol.libomv.transport.Failed
import org.gem.protocol.libomv.transport.LoggedOut
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.ProtocolHttpBody
import org.gem.protocol.libomv.transport.ProtocolHttpClient
import org.gem.protocol.libomv.transport.ProtocolHttpException
import org.gem.protocol.libomv.transport.ProtocolHttpRequest
import org.gem.protocol.libomv.transport.toSimulatorCircuit

class ProtocolLoginRuntime private constructor(
    private val clientSession: LibomvClientSession,
    private val httpClient: ProtocolHttpClient,
    private val circuitClient: ProtocolSimulatorCircuitClient,
    private val viewerIdentityProvider: GemViewerIdentityProvider,
    private val secretResolver: LoginSecretResolver,
    private val clockPort: ClockPort,
    private val machineIdentityProvider: GemMachineIdentityProvider,
    private val loginPackageBuilder: LoginPackageBuilder,
    private val loginPackageSerializer: LoginPackageSerializer,
) {
    internal constructor(
        clientSession: LibomvClientSession,
        httpClient: ProtocolHttpClient,
        circuitClient: ProtocolSimulatorCircuitClient,
        viewerIdentityProvider: GemViewerIdentityProvider,
        secretResolver: LoginSecretResolver,
        clockPort: ClockPort,
        machineIdentityProvider: GemMachineIdentityProvider,
        digestPort: Md5DigestPort,
    ) : this(
        clientSession = clientSession,
        httpClient = httpClient,
        circuitClient = circuitClient,
        viewerIdentityProvider = viewerIdentityProvider,
        secretResolver = secretResolver,
        clockPort = clockPort,
        machineIdentityProvider = machineIdentityProvider,
        loginPackageBuilder = LoginPackageBuilder(digestPort),
        loginPackageSerializer = LoginPackageSerializer,
    )

    fun login(request: LoginRequest): SessionLoginResult {
        val secret = secretResolver.resolve(request.credentialHandle)
            ?: return loginFailure("login secret unavailable")
        val viewerIdentity = try {
            viewerIdentityProvider.resolve()
        } catch (ex: RuntimeException) {
            return loginFailure("viewer identity unavailable")
        }
        val machineIdentity = try {
            machineIdentityProvider.resolve()
        } catch (ex: RuntimeException) {
            return loginFailure("viewer identity unavailable")
        }
        val loginPackage = loginPackageBuilder.build(secret, viewerIdentity, machineIdentity)
            ?: return loginFailure("login secret invalid")
        val response = try {
            httpClient.execute(loginHttpRequest(loginPackage))
        } catch (ex: ProtocolHttpException) {
            return loginFailure(transportFailure(ex.message))
        }

        if (response.statusCode !in 200..299) {
            return loginFailure(
                transportFailure(
                    "http_status=${response.statusCode}; ${bodyDiagnostic(response.body)}",
                ),
            )
        }

        return when (val mapped = LibomvLoginMapping.parse(response.body)) {
            is LibomvLoginMappingResult.Success -> {
                val session = GemSession(
                    sessionId = mapped.value.sessionId,
                    accountLabel = request.accountLabel,
                    startedAt = clockPort.now(),
                    isActive = true,
                )
                clientSession.activate(
                    session = session,
                    agentId = mapped.value.agentId,
                    seedCapability = mapped.value.seedCapability,
                    simulatorIp = mapped.value.simulatorIp,
                    simulatorPort = mapped.value.simulatorPort,
                    regionHandle = mapped.value.regionHandle,
                    circuitCode = mapped.value.circuitCode,
                    agentName = loginPackage.agentName(),
                    inventoryRoots = mapped.value.inventoryRoots,
                    appearanceState = mapped.value.appearanceState,
                )
                SessionLoginResult.Success(session)
            }
            is LibomvLoginMappingResult.Failure -> loginFailure(mapped.value.redactedMessage)
        }
    }

    fun logout(session: GemSession): SessionLogoutResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure ->
                return SessionLogoutResult.Failure(result.failure.copy(reason = CoreFailureReason.LOGOUT_FAILED))
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        return when (val result = circuitClient.logout(identity.toSimulatorCircuit())) {
            LoggedOut,
            ClosedWithoutReply,
            -> {
                clientSession.clear()
                SessionLogoutResult.LoggedOut
            }
            is Failed -> SessionLogoutResult.Failure(
                CoreFailure(CoreFailureReason.LOGOUT_FAILED, result.redactedMessage),
            )
        }
    }

    private fun loginHttpRequest(loginPackage: LoginPackage): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = loginPackage.loginUri,
        headers = mapOf("Content-Type" to "text/xml"),
        body = ProtocolHttpBody.TextBody(loginPackageSerializer.toXmlRpc(loginPackage), "text/xml"),
        timeout = loginPackage.timeout,
        redactionKeys = setOf(LoginKeys.SECRET, LoginKeys.MAC, LoginKeys.ID0),
    )

    private fun transportFailure(diagnostic: String?): String {
        val redacted = diagnostic
            ?.takeIf(String::isNotBlank)
            ?.let(SafeDiagnosticRedaction::redact)
            ?.takeIf(String::isNotBlank)
        return redacted
            ?.let { "${LibomvLoginFailureKind.TRANSPORT_FAILURE.redactedMessage}: $it" }
            ?: LibomvLoginFailureKind.TRANSPORT_FAILURE.redactedMessage
    }

    private fun bodyDiagnostic(body: ByteArray): String {
        val text = body.decodeToString()
        return SafeDiagnosticRedaction.excerpt(text)
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"
    }

    private fun loginFailure(message: String): SessionLoginResult.Failure =
        SessionLoginResult.Failure(CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = message))

    private fun LoginPackage.agentName(): String = "$first $last".trim()
}

fun interface LoginSecretResolver {
    fun resolve(handle: CredentialHandle): LoginSecret?

    companion object {
        fun unavailable(): LoginSecretResolver = LoginSecretResolver { null }
    }
}

class ProtocolLoginStartLocationProbe(
    private val secretResolver: LoginSecretResolver,
) {
    fun startLocation(handle: CredentialHandle): String? =
        secretResolver.resolve(handle)?.startLocation

    companion object {
        fun unavailable(): ProtocolLoginStartLocationProbe =
            ProtocolLoginStartLocationProbe(LoginSecretResolver.unavailable())
    }
}

data class LoginSecret(
    val loginUri: String,
    val firstName: String,
    val lastName: String,
    val sharedSecret: String,
    val startLocation: String = "last",
)
