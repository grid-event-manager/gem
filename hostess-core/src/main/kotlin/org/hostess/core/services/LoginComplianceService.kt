package org.hostess.core.services

import org.hostess.core.domain.LoginComplianceDecision
import org.hostess.core.domain.LoginComplianceReceipt
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.core.ports.LoginRequest

class LoginComplianceService {
    fun preflight(
        request: LoginRequest,
        compliance: LoginComplianceRequest,
    ): LoginComplianceDecision {
        val reasonCode = when {
            !compliance.proofAccountAttested -> "proof_account_attestation_missing"
            compliance.evidenceSource == ScriptedAgentEvidenceSource.ABSENT -> "scripted_agent_evidence_absent"
            compliance.automatedUse && !compliance.scriptedAgentAttested -> "scripted_agent_attestation_missing"
            else -> "allowed"
        }
        val receipt = LoginComplianceReceipt(
            proofAccountLabel = compliance.proofAccountLabel,
            scriptedAgentEvidencePresent = compliance.evidenceSource != ScriptedAgentEvidenceSource.ABSENT,
            evidenceSource = compliance.evidenceSource,
            automatedUse = compliance.automatedUse,
            operatorLabel = compliance.operatorLabel,
            reasonCode = reasonCode,
        )

        return if (reasonCode == "allowed") {
            LoginComplianceDecision.Allowed(receipt)
        } else {
            LoginComplianceDecision.Denied(receipt)
        }
    }
}
