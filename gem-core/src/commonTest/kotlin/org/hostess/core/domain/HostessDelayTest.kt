package org.hostess.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostessDelayTest {
    @Test
    fun `zero and positive delay helpers expose pacing state`() {
        assertTrue(HostessDelay.ZERO.isZero)
        assertFalse(HostessDelay.ZERO.isPositive)

        val delay = HostessDelay.ofSeconds(5)

        assertEquals(5_000L, delay.milliseconds)
        assertFalse(delay.isZero)
        assertTrue(delay.isPositive)
    }

    @Test
    fun `negative delay fails closed`() {
        assertFailsWith<IllegalArgumentException> {
            HostessDelay.ofMilliseconds(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            HostessDelay.ofSeconds(-1)
        }
    }
}
