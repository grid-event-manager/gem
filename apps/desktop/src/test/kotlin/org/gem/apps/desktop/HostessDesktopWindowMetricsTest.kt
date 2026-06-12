package org.gem.apps.desktop

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class HostessDesktopWindowMetricsTest {
    @Test
    fun `desktop window metrics project shared phone form tokens`() {
        assertEquals(440.dp, HostessDesktopWindowMetrics.initialWidth)
        assertEquals(780.dp, HostessDesktopWindowMetrics.initialHeight)
    }
}
