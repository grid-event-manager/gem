package org.gem.core.services

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.LoginRequest
import org.gem.core.ports.SessionLoginResult
import org.gem.core.testing.FakeRedactionPort
import org.gem.core.testing.FakeSessionPort
import org.gem.core.testing.failure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionServiceTest {
    @Test
    fun `login failure details are redacted before returning to callers`() {
        val redactionPort = FakeRedactionPort { value -> value.replace("credential-ref", "[redacted]") }
        val sessionPort = FakeSessionPort(
            loginResult = SessionLoginResult.Failure(
                failure(CoreFailureReason.LOGIN_FAILED, "credential-ref rejected"),
            ),
        )
        val service = SessionService(sessionPort, LoginComplianceService(), redactionPort)
        val request = loginRequest()

        val result = assertIs<SessionLoginResult.Failure>(
            service.login(request, allowedCompliance()),
        )

        assertEquals("credential-ref rejected", redactionPort.inputs.single())
        assertEquals("[redacted] rejected", result.failure.redactedMessage)
        assertEquals(listOf(request), sessionPort.loginRequests)
    }

    @Test
    fun `denied login compliance never calls session port`() {
        val redactionPort = FakeRedactionPort()
        val sessionPort = FakeSessionPort()
        val service = SessionService(sessionPort, LoginComplianceService(), redactionPort)

        val result = assertIs<SessionLoginResult.Failure>(
            service.login(
                loginRequest(),
                allowedCompliance(proofAccountAttested = false),
            ),
        )

        assertEquals(emptyList(), sessionPort.loginRequests)
        assertEquals(emptyList(), redactionPort.inputs)
        assertEquals(CoreFailureReason.LOGIN_FAILED, result.failure.reason)
        assertEquals("login compliance blocked: proof_account_attestation_missing", result.failure.redactedMessage)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("proof-account"),
        credentialHandle = CredentialHandle("credential-ref"),
    )

    private fun allowedCompliance(
        proofAccountAttested: Boolean = true,
    ): LoginComplianceRequest = LoginComplianceRequest(
        proofAccountAttested = proofAccountAttested,
        automatedUse = true,
        scriptedAgentAttested = true,
        operatorLabel = OperatorLabel("operator"),
        proofAccountLabel = "proof-account",
        evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
    )
}
