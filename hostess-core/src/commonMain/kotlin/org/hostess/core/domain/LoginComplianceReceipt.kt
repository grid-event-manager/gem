package org.hostess.core.domain

data class LoginComplianceReceipt(
    val proofAccountLabel: String,
    val scriptedAgentEvidencePresent: Boolean,
    val evidenceSource: ScriptedAgentEvidenceSource,
    val automatedUse: Boolean,
    val operatorLabel: OperatorLabel,
    val reasonCode: String,
)
