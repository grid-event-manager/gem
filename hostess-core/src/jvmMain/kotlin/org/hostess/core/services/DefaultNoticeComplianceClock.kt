package org.hostess.core.services

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.hostess.core.domain.NoticeLedgerDay

class DefaultNoticeComplianceClock private constructor(
    private val clock: Clock,
    private val currentDate: (() -> String)?,
) : NoticeComplianceClock {
    constructor(clock: Clock = Clock.systemUTC()) : this(clock, null)

    internal constructor(currentDate: () -> String) : this(Clock.systemUTC(), currentDate)

    override fun currentSecondLifeDay(): NoticeLedgerDay =
        NoticeLedgerDay(currentDate?.invoke() ?: LocalDate.now(clock.withZone(SECOND_LIFE_ZONE)).toString())

    private companion object {
        val SECOND_LIFE_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
    }
}
