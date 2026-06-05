package org.hostess.core.services

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.hostess.core.domain.NoticeDeliveryDay

class DefaultNoticeComplianceClock(
    private val clock: Clock = Clock.systemUTC(),
) : NoticeComplianceClock {
    override fun currentSecondLifeDay(): NoticeDeliveryDay =
        NoticeDeliveryDay(LocalDate.now(clock.withZone(SECOND_LIFE_ZONE)).toString())

    private companion object {
        val SECOND_LIFE_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
    }
}
