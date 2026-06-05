package org.hostess.core.ports

import org.hostess.core.domain.NoticeComplianceLedgerResult
import org.hostess.core.domain.NoticeDeliveryCount
import org.hostess.core.domain.NoticeDeliveryDay
import org.hostess.core.domain.NoticeDeliveryLedgerSnapshot
import org.hostess.core.domain.OperatorLabel

interface NoticeComplianceLedgerPort {
    fun snapshot(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>

    fun reserve(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        projected: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>

    fun recordSendResult(
        operatorLabel: OperatorLabel,
        deliveryDay: NoticeDeliveryDay,
        reservedProjection: NoticeDeliveryCount,
        delivered: NoticeDeliveryCount,
    ): NoticeComplianceLedgerResult<NoticeDeliveryLedgerSnapshot>
}
