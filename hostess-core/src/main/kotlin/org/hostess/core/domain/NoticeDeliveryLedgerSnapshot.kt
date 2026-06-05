package org.hostess.core.domain

data class NoticeDeliveryLedgerSnapshot(
    val operatorLabel: OperatorLabel,
    val deliveryDay: NoticeDeliveryDay,
    val reservedDeliveryCount: NoticeDeliveryCount,
    val recordedSentDeliveryCount: NoticeDeliveryCount,
)
