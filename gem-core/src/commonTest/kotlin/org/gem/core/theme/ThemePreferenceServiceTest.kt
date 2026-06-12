package org.gem.core.theme

import org.gem.core.testing.FakeThemePreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceServiceTest {
    @Test
    fun `loaded preference returns preference without warning`() {
        val service = service(ThemePreferenceLoadResult.Loaded(ThemePreference.DARK))

        assertEquals(ThemePreferenceSnapshot(ThemePreference.DARK), service.loadPreference())
    }

    @Test
    fun `missing preference defaults to system without warning`() {
        val service = service(ThemePreferenceLoadResult.Missing)

        assertEquals(ThemePreferenceSnapshot(ThemePreference.SYSTEM), service.loadPreference())
    }

    @Test
    fun `invalid preference defaults to system with warning`() {
        val service = service(ThemePreferenceLoadResult.InvalidValue("BLUE"))

        assertEquals(
            ThemePreferenceSnapshot(
                preference = ThemePreference.SYSTEM,
                warning = ThemePreferenceLoadWarning.InvalidValue("BLUE"),
            ),
            service.loadPreference(),
        )
    }

    @Test
    fun `storage failure defaults to system with warning`() {
        val service = service(ThemePreferenceLoadResult.StorageFailed("[redacted-storage]"))

        assertEquals(
            ThemePreferenceSnapshot(
                preference = ThemePreference.SYSTEM,
                warning = ThemePreferenceLoadWarning.StorageFailed("[redacted-storage]"),
            ),
            service.loadPreference(),
        )
    }

    @Test
    fun `save delegates preference and returns saved`() {
        val store = FakeThemePreferenceStore()
        val service = ThemePreferenceService(store)

        val result = service.savePreference(ThemePreference.LIGHT)

        assertEquals(ThemePreferenceSaveResult.Saved, result)
        assertEquals(listOf(ThemePreference.LIGHT), store.savedPreferences)
    }

    @Test
    fun `save storage failure is returned without pretending to save`() {
        val store = FakeThemePreferenceStore(
            saveResult = ThemePreferenceSaveResult.StorageFailed("[redacted-save]"),
        )
        val service = ThemePreferenceService(store)

        val result = service.savePreference(ThemePreference.DARK)

        assertEquals(ThemePreferenceSaveResult.StorageFailed("[redacted-save]"), result)
        assertEquals(listOf(ThemePreference.DARK), store.savedPreferences)
    }

    private fun service(loadResult: ThemePreferenceLoadResult): ThemePreferenceService =
        ThemePreferenceService(FakeThemePreferenceStore(loadResult = loadResult))
}
