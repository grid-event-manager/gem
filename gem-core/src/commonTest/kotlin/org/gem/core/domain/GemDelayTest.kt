package org.gem.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemDelayTest {
    @Test
    fun `zero and positive delay helpers expose pacing state`() {
        assertTrue(GemDelay.ZERO.isZero)
        assertFalse(GemDelay.ZERO.isPositive)

        val delay = GemDelay.ofSeconds(5)

        assertEquals(5_000L, delay.milliseconds)
        assertFalse(delay.isZero)
        assertTrue(delay.isPositive)
    }

    @Test
    fun `negative delay fails closed`() {
        assertFailsWith<IllegalArgumentException> {
            GemDelay.ofMilliseconds(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            GemDelay.ofSeconds(-1)
        }
    }
}
