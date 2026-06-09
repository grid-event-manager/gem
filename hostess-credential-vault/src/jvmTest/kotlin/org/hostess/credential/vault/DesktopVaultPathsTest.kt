package org.hostess.credential.vault

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopVaultPathsTest {
    @Test
    fun `selects linux xdg data home when present`() {
        val path = DesktopVaultPaths.defaultVaultDirectory(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to "/var/tmp/xdg"),
            userHome = "/home/hostess",
        )

        assertEquals("/var/tmp/xdg/Hostess/vault", path)
    }

    @Test
    fun `selects linux home fallback when xdg data home is absent`() {
        val path = DesktopVaultPaths.defaultVaultDirectory(
            osName = "Linux",
            env = emptyMap(),
            userHome = "/home/hostess",
        )

        assertEquals("/home/hostess/.local/share/Hostess/vault", path)
    }

    @Test
    fun `selects windows app data path`() {
        val path = DesktopVaultPaths.defaultVaultDirectory(
            osName = "Windows 11",
            env = mapOf("APPDATA" to """C:\Users\Hostess\AppData\Roaming"""),
            userHome = """C:\Users\Hostess""",
        )

        assertEquals("""C:\Users\Hostess\AppData\Roaming\Hostess\vault""", path)
    }

    @Test
    fun `selects mac application support path`() {
        val path = DesktopVaultPaths.defaultVaultDirectory(
            osName = "Mac OS X",
            env = emptyMap(),
            userHome = "/Users/hostess",
        )

        assertEquals("/Users/hostess/Library/Application Support/Hostess/vault", path)
    }
}
