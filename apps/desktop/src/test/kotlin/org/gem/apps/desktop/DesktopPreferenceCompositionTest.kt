package org.gem.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.preferences.DesktopGemPreferencePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPreferenceCompositionTest {
    @Test
    fun `opens desktop theme preference service at desktop preference path`() {
        val tempDataHome = Files.createTempDirectory("gem-desktop-preference-composition-test")
        try {
            val service = DesktopPreferenceComposition.open(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertEquals(ThemePreferenceSaveResult.Saved, service.savePreference(ThemePreference.LIGHT))
            assertEquals(ThemePreference.LIGHT, service.loadPreference().preference)
            assertTrue(Files.exists(Path.of(preferenceFile(tempDataHome))))
        } finally {
            DesktopTestDirectoryCleaner.deleteRecursively(tempDataHome)
        }
    }

    private fun preferenceFile(tempDataHome: Path): String =
        DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
            userHome = tempDataHome.resolve("home").toString(),
        )

}
