package org.hostess.ui.time

import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.ports.ClockPort
import kotlin.test.Test
import kotlin.test.assertEquals

class SecondLifeTimeServiceTest {
    @Test
    fun `current snapshot uses injected clock port`() {
        val service = SecondLifeTimeService(FixedClockPort(HostessInstant(1_781_209_800_000L)))

        val snapshot = service.currentSnapshot()

        assertEquals(1_781_209_800_000L, snapshot.epochMilliseconds)
        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 30, meridiem = SecondLifeMeridiem.PM), snapshot.display)
    }

    private class FixedClockPort(
        private val instant: HostessInstant,
    ) : ClockPort {
        override fun now(): HostessInstant = instant

        override fun pause(duration: HostessDelay) = Unit
    }
}
