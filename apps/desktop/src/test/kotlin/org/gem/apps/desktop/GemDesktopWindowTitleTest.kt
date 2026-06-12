package org.gem.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class GemDesktopWindowTitleTest {
    @Test
    fun `desktop window title uses package identity and visible version`() {
        assertEquals("gem 0.1.12", GemDesktopWindowTitle.current())
    }
}
