package org.gem.protocol.libomv

import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemSession
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.SessionLoginResult
import org.gem.core.ports.SessionLogoutResult
import org.gem.core.ports.SessionPort
import org.gem.protocol.libomv.runtime.ProtocolLoginRuntime

class LibomvSessionAdapter(
    internal val clientSession: LibomvClientSession,
    private val loginRuntime: ProtocolLoginRuntime? = null,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        loginRuntime?.login(request)
            ?: SessionLoginResult.Failure(clientSession.unavailable(CoreFailureReason.LOGIN_FAILED))

    override fun logout(session: GemSession): SessionLogoutResult =
        loginRuntime?.logout(session)
            ?: SessionLogoutResult.Failure(clientSession.unavailable(CoreFailureReason.LOGOUT_FAILED))
}
