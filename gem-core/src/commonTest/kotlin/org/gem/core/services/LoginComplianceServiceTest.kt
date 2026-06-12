package org.gem.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.gem.core.domain.AccountLabel
import org.gem.core.domain.LoginComplianceDecision
import org.gem.core.domain.LoginComplianceRequest
import org.gem.core.domain.OperatorLabel
import org.gem.core.domain.ScriptedAgentEvidenceSource
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.LoginRequest

class LoginComplianceServiceTest {
    private val service = LoginComplianceService()

    @Test
    fun `allows complete proof account and scripted agent evidence`() {
        val decision = service.preflight(loginRequest(), compliance())

        val allowed = assertIs<LoginComplianceDecision.Allowed>(decision)
        assertEquals("allowed", allowed.receipt.reasonCode)
        assertEquals("proof-account", allowed.receipt.proofAccountLabel)
        assertEquals(true, allowed.receipt.scriptedAgentEvidencePresent)
        assertEquals(ScriptedAgentEvidenceSource.OPERATOR_ATTESTED, allowed.receipt.evidenceSource)
        assertEquals(true, allowed.receipt.automatedUse)
        assertEquals(OperatorLabel("operator"), allowed.receipt.operatorLabel)
    }

    @Test
    fun `denies missing proof account attestation before other checks`() {
        val decision = service.preflight(
            loginRequest(),
            compliance(
                proofAccountAttested = false,
                evidenceSource = ScriptedAgentEvidenceSource.ABSENT,
                scriptedAgentAttested = false,
            ),
        )

        assertDenied("proof_account_attestation_missing", decision)
    }

    @Test
    fun `denies absent scripted agent evidence`() {
        val decision = service.preflight(
            loginRequest(),
            compliance(evidenceSource = ScriptedAgentEvidenceSource.ABSENT),
        )

        assertDenied("scripted_agent_evidence_absent", decision)
    }

    @Test
    fun `denies automated use without scripted agent attestation`() {
        val decision = service.preflight(
            loginRequest(),
            compliance(scriptedAgentAttested = false),
        )

        assertDenied("scripted_agent_attestation_missing", decision)
    }

    @Test
    fun `operator label rejects blank and untrimmed values`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { OperatorLabel(" ") }
        kotlin.test.assertFailsWith<IllegalArgumentException> { OperatorLabel(" operator ") }
    }

    private fun assertDenied(
        reasonCode: String,
        decision: LoginComplianceDecision,
    ) {
        val denied = assertIs<LoginComplianceDecision.Denied>(decision)
        assertEquals(reasonCode, denied.receipt.reasonCode)
    }

    private fun loginRequest(): LoginRequest = LoginRequest(
        accountLabel = AccountLabel("proof-account"),
        credentialHandle = CredentialHandle("credential-ref"),
    )

    private fun compliance(
        proofAccountAttested: Boolean = true,
        automatedUse: Boolean = true,
        scriptedAgentAttested: Boolean = true,
        evidenceSource: ScriptedAgentEvidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
    ): LoginComplianceRequest = LoginComplianceRequest(
        proofAccountAttested = proofAccountAttested,
        automatedUse = automatedUse,
        scriptedAgentAttested = scriptedAgentAttested,
        operatorLabel = OperatorLabel("operator"),
        proofAccountLabel = "proof-account",
        evidenceSource = evidenceSource,
    )
}
