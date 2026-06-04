package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession

class LibomvClientSession private constructor(
    private val protocolAvailable: Boolean,
    private var activeSession: HostessSession?,
    private var privateEndpoint: String?,
) {
    fun unavailable(reason: CoreFailureReason): CoreFailure =
        CoreFailure(reason, redactedMessage = if (protocolAvailable) {
            "protocol runtime unavailable"
        } else {
            "protocol bootstrap unavailable"
        })

    fun isProtocolAvailable(): Boolean = protocolAvailable

    internal fun activate(session: HostessSession, privateEndpoint: String? = null) {
        activeSession = session
        this.privateEndpoint = privateEndpoint
    }

    internal fun clear() {
        activeSession = null
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

    companion object {
        fun unavailable(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = false,
            activeSession = null,
            privateEndpoint = null,
        )

        fun inactive(): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = null,
            privateEndpoint = null,
        )

        internal fun active(session: HostessSession): LibomvClientSession = LibomvClientSession(
            protocolAvailable = true,
            activeSession = session,
            privateEndpoint = null,
        )
    }
}
