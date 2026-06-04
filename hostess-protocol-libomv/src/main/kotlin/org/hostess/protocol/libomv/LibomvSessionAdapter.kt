package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort

class LibomvSessionAdapter(
    internal val clientSession: LibomvClientSession,
) : SessionPort {
    override fun login(request: LoginRequest): SessionLoginResult =
        SessionLoginResult.Failure(clientSession.unavailable(CoreFailureReason.LOGIN_FAILED))

    override fun logout(session: HostessSession): SessionLogoutResult =
        SessionLogoutResult.Failure(clientSession.unavailable(CoreFailureReason.LOGOUT_FAILED))
}
