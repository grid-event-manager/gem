package org.hostess.ui.time

import org.hostess.core.domain.HostessInstant
import org.hostess.ui.text.EnglishHostessTextCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals

class SecondLifeTimeFormatterTest {
    @Test
    fun `formats current second life time from pacific daylight time`() {
        val display = SecondLifeTimeFormatter.display(HostessInstant(1_781_209_800_000L))

        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 30, meridiem = SecondLifeMeridiem.PM), display)
        assertEquals("SLT 1:30 PM", display.label(EnglishHostessTextCatalogue))
    }

    @Test
    fun `applies spring daylight transition at second sunday in march`() {
        val before = SecondLifeTimeFormatter.display(HostessInstant(1_772_963_940_000L))
        val after = SecondLifeTimeFormatter.display(HostessInstant(1_772_964_000_000L))

        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 59, meridiem = SecondLifeMeridiem.AM), before)
        assertEquals(SecondLifeTimeDisplay(hour = 3, minute = 0, meridiem = SecondLifeMeridiem.AM), after)
    }

    @Test
    fun `applies autumn standard transition at first sunday in november`() {
        val before = SecondLifeTimeFormatter.display(HostessInstant(1_793_523_540_000L))
        val after = SecondLifeTimeFormatter.display(HostessInstant(1_793_523_600_000L))

        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 59, meridiem = SecondLifeMeridiem.AM), before)
        assertEquals(SecondLifeTimeDisplay(hour = 1, minute = 0, meridiem = SecondLifeMeridiem.AM), after)
    }
}
