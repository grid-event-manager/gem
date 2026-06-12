package org.gem.ui.time

import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.ports.ClockPort
import kotlin.test.Test
import kotlin.test.assertEquals

class SecondLifeTimeServiceTest {
    @Test
    fun `current snapshot uses injected clock port`() {
        val service = SecondLifeTimeService(FixedClockPort(GemInstant(1_781_209_800_000L)))

        val snapshot = service.currentSnapshot()

        assertEquals(1_781_209_800_000L, snapshot.epochMilliseconds)
        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 30, meridiem = SecondLifeMeridiem.PM), snapshot.display)
    }

    private class FixedClockPort(
        private val instant: GemInstant,
    ) : ClockPort {
        override fun now(): GemInstant = instant

        override fun pause(duration: GemDelay) = Unit
    }
}
