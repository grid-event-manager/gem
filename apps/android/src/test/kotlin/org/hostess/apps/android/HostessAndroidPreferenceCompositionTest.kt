package org.hostess.apps.android

import java.nio.file.Files
import java.nio.file.Path
import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.preferences.AndroidHostessPreferencePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostessAndroidPreferenceCompositionTest {
    @Test
    fun `opens android theme preference service at app files preference path`() {
        val appFilesDir = Files.createTempDirectory("hostess-android-preference-composition-test")
        try {
            val service = HostessAndroidPreferenceComposition.open(appFilesDir.toFile())

            assertEquals(ThemePreferenceSaveResult.Saved, service.savePreference(ThemePreference.DARK))
            assertEquals(ThemePreference.DARK, service.loadPreference().preference)
            assertTrue(Files.exists(Path.of(preferenceFile(appFilesDir))))
        } finally {
            HostessAndroidTestDirectoryCleaner.deleteRecursively(appFilesDir)
        }
    }

    private fun preferenceFile(appFilesDir: Path): String =
        AndroidHostessPreferencePaths.defaultPreferenceFile(appFilesDir.toString())

}
