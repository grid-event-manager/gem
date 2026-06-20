package org.gem.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSystemDarkThemeDetectorTest {
    @Test
    fun linuxPrefersDarkFromGsettingsColorScheme() {
        val result = JvmSystemDarkThemeDetector.prefersDarkTheme(
            osName = "Linux",
            commandRunner = { command ->
                when (command) {
                    listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme") -> "'prefer-dark'"
                    else -> null
                }
            },
        )

        assertEquals(true, result)
    }

    @Test
    fun linuxFallsBackToGtkThemeNameFromGsettings() {
        val result = JvmSystemDarkThemeDetector.prefersDarkTheme(
            osName = "Linux",
            commandRunner = { command ->
                when (command) {
                    listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme") -> "'default'"
                    listOf("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme") -> "'Mint-Y-Dark-Aqua'"
                    else -> null
                }
            },
        )

        assertEquals(true, result)
    }

    @Test
    fun macPrefersDarkFromAppleInterfaceStyle() {
        val result = JvmSystemDarkThemeDetector.prefersDarkTheme(
            osName = "Mac OS X",
            commandRunner = { command ->
                when (command) {
                    listOf("defaults", "read", "-g", "AppleInterfaceStyle") -> "Dark"
                    else -> null
                }
            },
        )

        assertEquals(true, result)
    }

    @Test
    fun windowsPrefersDarkWhenAppsUseLightThemeIsZero() {
        val result = JvmSystemDarkThemeDetector.prefersDarkTheme(
            osName = "Windows 11",
            commandRunner = { "AppsUseLightTheme    REG_DWORD    0x0" },
        )

        assertEquals(true, result)
    }

    @Test
    fun unsupportedOsReturnsNull() {
        assertNull(
            JvmSystemDarkThemeDetector.prefersDarkTheme(
                osName = "Plan 9",
                commandRunner = { null },
            ),
        )
    }
}
