package org.hostess.protocol.libomv.runtime

import java.time.Clock
import java.time.Duration
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvLoginMapping
import org.hostess.protocol.libomv.mapping.LibomvLoginMappingResult
import org.hostess.protocol.libomv.mapping.LoginKeys
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest

class ProtocolLoginRuntime(
    private val clientSession: LibomvClientSession,
    private val httpClient: ProtocolHttpClient,
    private val viewerIdentityProvider: HostessViewerIdentityProvider,
    private val secretResolver: LoginSecretResolver = LoginSecretResolver.unavailable(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun login(request: LoginRequest): SessionLoginResult {
        val secret = secretResolver.resolve(request.credentialHandle)
            ?: return loginFailure("login secret unavailable")
        val viewerIdentity = try {
            viewerIdentityProvider.resolve()
        } catch (ex: RuntimeException) {
            return loginFailure("viewer identity unavailable")
        }
        val response = try {
            httpClient.execute(loginHttpRequest(secret, viewerIdentity))
        } catch (ex: ProtocolHttpException) {
            return loginFailure("login transport failed")
        }

        if (response.statusCode !in 200..299) {
            return loginFailure("login transport failed")
        }

        return when (val mapped = LibomvLoginMapping.parse(response.body)) {
            is LibomvLoginMappingResult.Success -> {
                val session = HostessSession(
                    sessionId = mapped.value.sessionId,
                    accountLabel = request.accountLabel,
                    startedAt = clock.instant(),
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
            is LibomvLoginMappingResult.Failure -> loginFailure("login failed")
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

    private fun loginHttpRequest(secret: LoginSecret, viewerIdentity: HostessViewerIdentity): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = secret.loginUri,
        headers = mapOf("Content-Type" to "application/llsd+xml"),
        body = ProtocolHttpBody.TextBody(loginBody(secret, viewerIdentity), "application/llsd+xml"),
        timeout = Duration.ofSeconds(30),
        redactionKeys = setOf(LoginKeys.SECRET, LoginKeys.MAC, LoginKeys.ID0, LoginKeys.HOST_ID, LoginKeys.TOKEN),
    )

    private fun loginBody(secret: LoginSecret, viewerIdentity: HostessViewerIdentity): String = buildString {
        append("<llsd><map>")
        field("first", secret.firstName)
        field("last", secret.lastName)
        field(LoginKeys.SECRET, secret.sharedSecret)
        field("start", secret.startLocation)
        field(LoginKeys.CHANNEL, viewerIdentity.channel)
        field(LoginKeys.VERSION, viewerIdentity.version)
        field(LoginKeys.PLATFORM, viewerIdentity.platform.platform)
        field(LoginKeys.PLATFORM_VERSION, viewerIdentity.platform.platformVersion)
        field(LoginKeys.PLATFORM_STRING, viewerIdentity.platform.platformString)
        field(LoginKeys.MAC, viewerIdentity.host.mac)
        field(LoginKeys.ID0, viewerIdentity.host.id0)
        field(LoginKeys.HOST_ID, viewerIdentity.host.hostId)
        field(LoginKeys.TOKEN, "")
        booleanField(LoginKeys.AGREE_TO_TOS, false)
        booleanField(LoginKeys.READ_CRITICAL, false)
        booleanField(LoginKeys.EXTENDED_ERRORS, true)
        append("<key>options</key><array>")
        option("inventory-root")
        option("inventory-skeleton")
        option("initial-outfit")
        option("gestures")
        option("max-agent-groups")
        append("</array>")
        append("</map></llsd>")
    }

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(escapeXml(key)).append("</key><string>")
            .append(escapeXml(value))
            .append("</string>")
    }

    private fun StringBuilder.option(value: String) {
        append("<string>").append(escapeXml(value)).append("</string>")
    }

    private fun StringBuilder.booleanField(key: String, value: Boolean) {
        append("<key>").append(escapeXml(key)).append("</key><boolean>")
            .append(if (value) "1" else "0")
            .append("</boolean>")
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

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
