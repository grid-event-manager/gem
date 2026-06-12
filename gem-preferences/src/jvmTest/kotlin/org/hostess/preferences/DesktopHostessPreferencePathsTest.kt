package org.hostess.preferences

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopHostessPreferencePathsTest {
    @Test
    fun `selects linux xdg data home when present`() {
        val path = DesktopHostessPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to "/var/tmp/xdg"),
            userHome = "/home/hostess",
        )

        assertEquals("/var/tmp/xdg/Hostess/preferences/ui.properties", path)
    }

    @Test
    fun `selects linux home fallback when xdg data home is absent`() {
        val path = DesktopHostessPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = emptyMap(),
            userHome = "/home/hostess",
        )

        assertEquals("/home/hostess/.local/share/Hostess/preferences/ui.properties", path)
    }

    @Test
    fun `selects windows app data path and preserves backslashes`() {
        val path = DesktopHostessPreferencePaths.defaultPreferenceFile(
            osName = "Windows 11",
            env = mapOf("APPDATA" to """C:\Users\Hostess\AppData\Roaming"""),
            userHome = """C:\Users\Hostess""",
        )

        assertEquals("""C:\Users\Hostess\AppData\Roaming\Hostess\preferences\ui.properties""", path)
    }

    @Test
    fun `selects mac application support path`() {
        val path = DesktopHostessPreferencePaths.defaultPreferenceFile(
            osName = "Mac OS X",
            env = emptyMap(),
            userHome = "/Users/hostess",
        )

        assertEquals("/Users/hostess/Library/Application Support/Hostess/preferences/ui.properties", path)
    }
}
