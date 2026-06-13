package org.gem.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class GemDesktopWindowTitleTest {
    @Test
    fun `desktop window title uses package identity and visible version`() {
        assertEquals("GEM 0.1.18", GemDesktopWindowTitle.current())
    }
}
