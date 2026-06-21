package org.gem.apps.android

import java.nio.file.Files
import java.nio.file.Path
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSaveResult
import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceSaveResult
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

    @Test
    fun `opens android appearance profile service at app files appearance path`() {
        val appFilesDir = Files.createTempDirectory("gem-android-appearance-composition-test")
        try {
            val service = GemAndroidPreferenceComposition.openAppearanceProfiles(appFilesDir.toFile())

            val result = service.saveProfile(
                name = AppearanceProfileName("Proof"),
                mode = AppearanceMode.DARK,
                draft = stockDraft(AppearanceMode.DARK),
            )

            assertTrue(result is AppearanceProfileSaveResult.Saved)
            assertTrue(Files.exists(Path.of(appearanceProfileFile(appFilesDir))))
        } finally {
            GemAndroidTestDirectoryCleaner.deleteRecursively(appFilesDir)
        }
    }

    @Test
    fun `opens android language preference service at app files language path`() {
        val appFilesDir = Files.createTempDirectory("gem-android-language-composition-test")
        try {
            val service = GemAndroidPreferenceComposition.openLanguagePreference(appFilesDir.toFile())

            assertEquals(LanguagePreferenceSaveResult.Saved, service.savePreference(LanguagePreference.Locale("uk-UA")))
            assertEquals(LanguagePreference.Locale("uk-UA"), service.loadPreference().preference)
            assertTrue(Files.exists(Path.of(languagePreferenceFile(appFilesDir))))
        } finally {
            GemAndroidTestDirectoryCleaner.deleteRecursively(appFilesDir)
        }
    }

    private fun preferenceFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultPreferenceFile(appFilesDir.toString())

    private fun appearanceProfileFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultAppearanceProfileFile(appFilesDir.toString())

    private fun languagePreferenceFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultLanguagePreferenceFile(appFilesDir.toString())

    private fun stockDraft(mode: AppearanceMode): AppearanceDraft =
        AppearanceDraft.fromProfile(AppearanceProfileCatalogue.stockProfiles().first { it.mode == mode })

}
