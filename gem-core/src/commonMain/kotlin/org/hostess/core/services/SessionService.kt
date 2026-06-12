package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.LoginComplianceDecision
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.RedactionPort
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.ports.SessionLogoutResult
import org.hostess.core.ports.SessionPort

class SessionService(
    private val sessionPort: SessionPort,
    private val loginComplianceService: LoginComplianceService,
    private val redactionPort: RedactionPort,
) {
    fun login(
        request: LoginRequest,
        compliance: LoginComplianceRequest,
    ): SessionLoginResult =
        when (val decision = loginComplianceService.preflight(request, compliance)) {
            is LoginComplianceDecision.Allowed -> sessionPort.loginRedacted(request)
            is LoginComplianceDecision.Denied -> SessionLoginResult.Failure(
                CoreFailure(
                    reason = CoreFailureReason.LOGIN_FAILED,
                    redactedMessage = "login compliance blocked: ${decision.receipt.reasonCode}",
                ),
            )
        }

    fun logout(session: HostessSession): SessionLogoutResult =
        when (val result = sessionPort.logout(session)) {
            SessionLogoutResult.LoggedOut -> result
            is SessionLogoutResult.Failure -> SessionLogoutResult.Failure(result.failure.redacted())
        }

    private fun CoreFailure.redacted(): CoreFailure =
        copy(redactedMessage = redactedMessage?.let(redactionPort::redact))

    private fun SessionPort.loginRedacted(request: LoginRequest): SessionLoginResult =
        when (val result = login(request)) {
            is SessionLoginResult.Success -> result
            is SessionLoginResult.Failure -> SessionLoginResult.Failure(result.failure.redacted())
        }
}
