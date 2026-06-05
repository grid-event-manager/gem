package org.hostess.core.domain

data class NoticeComplianceRequest(
    val operatorLabel: OperatorLabel,
    val recipientEstimates: List<NoticeRecipientEstimate>,
) {
    init {
        require(recipientEstimates.isNotEmpty()) {
            "NoticeComplianceRequest requires recipient estimates."
        }
    }
}
