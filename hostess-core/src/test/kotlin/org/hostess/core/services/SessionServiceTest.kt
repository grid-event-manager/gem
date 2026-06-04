package org.hostess.core.services

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.core.ports.SessionLoginResult
import org.hostess.core.testing.FakeRedactionPort
import org.hostess.core.testing.FakeSessionPort
import org.hostess.core.testing.failure
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
        val service = SessionService(sessionPort, redactionPort)

        val result = assertIs<SessionLoginResult.Failure>(
            service.login(
                LoginRequest(
                    accountLabel = AccountLabel("proof-account"),
                    credentialHandle = CredentialHandle("credential-ref"),
                ),
            ),
        )

        assertEquals("credential-ref rejected", redactionPort.inputs.single())
        assertEquals("[redacted] rejected", result.failure.redactedMessage)
    }
}
