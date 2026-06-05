package org.hostess.core.services

import org.hostess.core.domain.NoticeDeliveryDay

fun interface NoticeComplianceClock {
    fun currentSecondLifeDay(): NoticeDeliveryDay
}
