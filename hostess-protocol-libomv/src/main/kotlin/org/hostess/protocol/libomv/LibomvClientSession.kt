package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession

class LibomvClientSession private constructor(
    private val protocolAvailable: Boolean,
    private var activeSession: HostessSession?,
    private var agentId: String?,
    private var privateEndpoint: String?,
) {
    fun unavailable(reason: CoreFailureReason): CoreFailure =
        CoreFailure(reason, redactedMessage = if (protocolAvailable) {
            "protocol runtime unavailable"
        } else {
            "protocol bootstrap unavailable"
        })

    fun isProtocolAvailable(): Boolean = protocolAvailable

    internal fun activate(
        session: HostessSession,
        agentId: String? = null,
        privateEndpoint: String? = null,
    ) {
        activeSession = session
        this.agentId = agentId
        this.privateEndpoint = privateEndpoint
    }

    internal fun clear() {
        activeSession = null
        agentId = null
        privateEndpoint = null
    }

    internal fun requireSession(session: HostessSession): CoreFailure? {
        if (!protocolAvailable) {
            return CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol bootstrap unavailable")
        }
        val active = activeSession
            ?: return CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol session inactive")
        return when {
            !active.isActive -> CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol session inactive")
            !session.isActive -> CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "hostess session inactive")
            active.sessionId != session.sessionId ->
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "hostess session mismatch")
            else -> null
        }
    }

    internal fun requireIdentity(session: HostessSession): LibomvSessionIdentityResult {
        val bindingFailure = requireSession(session)
        if (bindingFailure != null) {
            return LibomvSessionIdentityResult.Failure(bindingFailure)
        }
        val activeAgentId = agentId?.takeIf(String::isNotBlank)
            ?: return LibomvSessionIdentityResult.Failure(
                CoreFailure(CoreFailureReason.LOGIN_FAILED, redactedMessage = "protocol agent identity unavailable"),
            )
        return LibomvSessionIdentityResult.Success(
            LibomvSessionIdentity(
                agentId = activeAgentId,
                sessionId = session.sessionId.value,
            ),
        )
    }

    companion object {
        fun unavailable(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = false,
            activeSession = null,
            agentId = null,
            privateEndpoint = null,
        )

        fun inactive(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = null,
            agentId = null,
            privateEndpoint = null,
        )

        internal fun active(
            session: HostessSession,
            agentId: String? = null,
        ): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = session,
            agentId = agentId,
            privateEndpoint = null,
        )
    }
}

internal data class LibomvSessionIdentity(
    val agentId: String,
    val sessionId: String,
)

internal sealed interface LibomvSessionIdentityResult {
    data class Success(val identity: LibomvSessionIdentity) : LibomvSessionIdentityResult
    data class Failure(val failure: CoreFailure) : LibomvSessionIdentityResult
}
