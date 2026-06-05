package org.hostess.core.services

import kotlin.test.Test
import kotlin.test.assertEquals
import org.hostess.core.domain.NoticeDeliveryDay

class DefaultNoticeComplianceClockTest {
    @Test
    fun `maps current date source to Second Life delivery day`() {
        val beforeMidnight = DefaultNoticeComplianceClock { "2026-06-04" }
        val afterMidnight = DefaultNoticeComplianceClock { "2026-06-05" }

        assertEquals(NoticeDeliveryDay("2026-06-04"), beforeMidnight.currentSecondLifeDay())
        assertEquals(NoticeDeliveryDay("2026-06-05"), afterMidnight.currentSecondLifeDay())
    }
}
