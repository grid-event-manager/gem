package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort

class SessionService(
    private val sessionPort: SessionPort,
) {
    fun login(request: LoginRequest): SessionLoginResult = sessionPort.login(request)

    fun logout(session: HostessSession): SessionLogoutResult = sessionPort.logout(session)
}
