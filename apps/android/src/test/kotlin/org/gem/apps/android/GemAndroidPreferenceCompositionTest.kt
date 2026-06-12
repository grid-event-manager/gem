package org.gem.apps.android

import java.nio.file.Files
import java.nio.file.Path
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.preferences.AndroidGemPreferencePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GemAndroidPreferenceCompositionTest {
    @Test
    fun `opens android theme preference service at app files preference path`() {
        val appFilesDir = Files.createTempDirectory("gem-android-preference-composition-test")
        try {
            val service = GemAndroidPreferenceComposition.open(appFilesDir.toFile())

            assertEquals(ThemePreferenceSaveResult.Saved, service.savePreference(ThemePreference.DARK))
            assertEquals(ThemePreference.DARK, service.loadPreference().preference)
            assertTrue(Files.exists(Path.of(preferenceFile(appFilesDir))))
        } finally {
            GemAndroidTestDirectoryCleaner.deleteRecursively(appFilesDir)
        }
    }

    private fun preferenceFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultPreferenceFile(appFilesDir.toString())

}
