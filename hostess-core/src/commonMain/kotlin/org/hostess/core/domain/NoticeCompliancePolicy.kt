package org.hostess.core.domain

data class NoticeCompliancePolicy(
    val policyMaximum: NoticeDeliveryCount = NoticeDeliveryCount(5_000),
    val hostessHardCap: NoticeDeliveryCount = NoticeDeliveryCount(4_500),
) {
    init {
        require(hostessHardCap.value <= policyMaximum.value) {
            "Hostess hard cap cannot exceed policy maximum."
        }
    }
}
