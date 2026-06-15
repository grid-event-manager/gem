package org.gem.apps.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class GemDesktopWindowTitleTest {
    @Test
    fun `desktop window title uses package identity and visible version`() {
        assertEquals("GEM 0.1.31", GemDesktopWindowTitle.current())
    }

    @Test
    fun `desktop window title prefers packaged app version property`() {
        val original = System.getProperty("jpackage.app-version")
        try {
            System.setProperty("jpackage.app-version", "0.1.99")
            assertEquals("GEM 0.1.99", GemDesktopWindowTitle.current())
        } finally {
            if (original == null) {
                System.clearProperty("jpackage.app-version")
            } else {
                System.setProperty("jpackage.app-version", original)
            }
        }
    }
}
