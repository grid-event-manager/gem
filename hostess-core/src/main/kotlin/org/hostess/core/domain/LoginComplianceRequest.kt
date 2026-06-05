package org.hostess.core.domain

data class LoginComplianceRequest(
    val proofAccountAttested: Boolean,
    val automatedUse: Boolean,
    val scriptedAgentAttested: Boolean,
    val operatorLabel: OperatorLabel,
    val proofAccountLabel: String,
    val evidenceSource: ScriptedAgentEvidenceSource,
) {
    init {
        require(proofAccountLabel.isNotBlank()) { "proofAccountLabel cannot be blank." }
    }
}
