package org.hostess.protocol.libomv.runtime

import java.nio.charset.StandardCharsets
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.ClockPort
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.services.SafeDiagnosticRedaction
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvLoginFailureKind
import org.hostess.protocol.libomv.mapping.LibomvLoginMapping
import org.hostess.protocol.libomv.mapping.LibomvLoginMappingResult
import org.hostess.protocol.libomv.mapping.LoginKeys
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest

class ProtocolLoginRuntime private constructor(
    private val clientSession: LibomvClientSession,
    private val httpClient: ProtocolHttpClient,
    private val viewerIdentityProvider: HostessViewerIdentityProvider,
    private val secretResolver: LoginSecretResolver,
    private val clockPort: ClockPort,
    private val machineIdentityProvider: HostessMachineIdentityProvider,
    private val loginPackageBuilder: LoginPackageBuilder,
    private val loginPackageSerializer: LoginPackageSerializer,
) {
    internal constructor(
        clientSession: LibomvClientSession,
        httpClient: ProtocolHttpClient,
        viewerIdentityProvider: HostessViewerIdentityProvider,
        secretResolver: LoginSecretResolver,
        clockPort: ClockPort,
        machineIdentityProvider: HostessMachineIdentityProvider,
        digestPort: Md5DigestPort,
    ) : this(
        clientSession = clientSession,
        httpClient = httpClient,
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
                val session = HostessSession(
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
                )
                SessionLoginResult.Success(session)
            }
            is LibomvLoginMappingResult.Failure -> loginFailure(mapped.value.redactedMessage)
        }
    }

    fun logout(session: HostessSession): SessionLogoutResult {
        val bindingFailure = clientSession.requireSession(session)
        if (bindingFailure != null) {
            return SessionLogoutResult.Failure(bindingFailure.copy(reason = CoreFailureReason.LOGOUT_FAILED))
        }
        clientSession.clear()
        return SessionLogoutResult.LoggedOut
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
        val text = body.toString(StandardCharsets.UTF_8)
        return SafeDiagnosticRedaction.excerpt(text)
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"
    }

    private fun loginFailure(message: String): SessionLoginResult.Failure =
        SessionLoginResult.Failure(CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = message))
}

fun interface LoginSecretResolver {
    fun resolve(handle: CredentialHandle): LoginSecret?

    companion object {
        fun unavailable(): LoginSecretResolver = LoginSecretResolver { null }
    }
}

data class LoginSecret(
    val loginUri: String,
    val firstName: String,
    val lastName: String,
    val sharedSecret: String,
    val startLocation: String = "last",
)
