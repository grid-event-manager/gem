package org.gem.preferences

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopGemPreferencePathsTest {
    @Test
    fun `selects linux xdg data home when present`() {
        val path = DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to "/var/tmp/xdg"),
            userHome = "/home/gemuser",
        )

        assertEquals("/var/tmp/xdg/gem/preferences/ui.properties", path)
    }

    @Test
    fun `places appearance profile file beside desktop ui preferences`() {
        val path = DesktopGemPreferencePaths.defaultAppearanceProfileFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to "/var/tmp/xdg"),
            userHome = "/home/gemuser",
        )

        assertEquals("/var/tmp/xdg/gem/preferences/appearance-profiles.properties", path)
    }

    @Test
    fun `selects linux home fallback when xdg data home is absent`() {
        val path = DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = emptyMap(),
            userHome = "/home/gemuser",
        )

        assertEquals("/home/gemuser/.local/share/gem/preferences/ui.properties", path)
    }

    @Test
    fun `selects windows app data path and preserves backslashes`() {
        val path = DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Windows 11",
            env = mapOf("APPDATA" to """C:\Users\GemUser\AppData\Roaming"""),
            userHome = """C:\Users\GemUser""",
        )

        assertEquals("""C:\Users\GemUser\AppData\Roaming\gem\preferences\ui.properties""", path)
    }

    @Test
    fun `selects mac application support path`() {
        val path = DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Mac OS X",
            env = emptyMap(),
            userHome = "/Users/gemuser",
        )

        assertEquals("/Users/gemuser/Library/Application Support/gem/preferences/ui.properties", path)
    }
}
