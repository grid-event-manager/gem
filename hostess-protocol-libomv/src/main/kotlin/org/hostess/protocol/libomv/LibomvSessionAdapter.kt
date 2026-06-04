package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime

class LibomvSessionAdapter(
    internal val clientSession: LibomvClientSession,
    private val loginRuntime: ProtocolLoginRuntime? = null,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        loginRuntime?.login(request)
            ?: SessionLoginResult.Failure(clientSession.unavailable(CoreFailureReason.LOGIN_FAILED))

    override fun logout(session: HostessSession): SessionLogoutResult =
        loginRuntime?.logout(session)
            ?: SessionLogoutResult.Failure(clientSession.unavailable(CoreFailureReason.LOGOUT_FAILED))
}
