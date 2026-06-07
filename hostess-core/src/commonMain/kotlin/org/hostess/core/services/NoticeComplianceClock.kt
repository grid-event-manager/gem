package org.hostess.core.services

import org.hostess.core.domain.NoticeLedgerDay

fun interface NoticeComplianceClock {
    fun currentSecondLifeDay(): NoticeLedgerDay
}
