package org.hostess.core.domain

data class NoticeComplianceReceipt(
    val operatorLabel: OperatorLabel,
    val deliveryDay: NoticeDeliveryDay,
    val projectedDeliveryCount: NoticeDeliveryCount,
    val previousLedgerCount: NoticeDeliveryCount,
    val projectedLedgerTotal: NoticeDeliveryCount,
    val hardCap: NoticeDeliveryCount,
    val reasonCode: String,
    val redactedSourceSummary: String,
)
