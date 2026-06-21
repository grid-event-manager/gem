package org.gem.core.language

import kotlin.test.Test
import kotlin.test.assertEquals
import org.gem.core.testing.FakeLanguagePreferenceStore

class LanguagePreferenceServiceTest {
    @Test
    fun loadedPreferenceReturnsPreferenceWithoutWarning() {
        val service = service(LanguagePreferenceLoadResult.Loaded(LanguagePreference.Locale("fr-FR")))

        assertEquals(LanguagePreferenceSnapshot(LanguagePreference.Locale("fr-FR")), service.loadPreference())
    }

    @Test
    fun missingPreferenceDefaultsToSystemWithoutWarning() {
        val service = service(LanguagePreferenceLoadResult.Missing)

        assertEquals(LanguagePreferenceSnapshot(LanguagePreference.System), service.loadPreference())
    }

    @Test
    fun invalidPreferenceDefaultsToSystemWithWarning() {
        val service = service(LanguagePreferenceLoadResult.InvalidValue("AUTO"))

        assertEquals(
            LanguagePreferenceSnapshot(
                preference = LanguagePreference.System,
                warning = LanguagePreferenceLoadWarning.InvalidValue("AUTO"),
            ),
            service.loadPreference(),
        )
    }

    @Test
    fun storageFailureDefaultsToSystemWithWarning() {
        val service = service(LanguagePreferenceLoadResult.StorageFailed("[redacted-storage]"))

        assertEquals(
            LanguagePreferenceSnapshot(
                preference = LanguagePreference.System,
                warning = LanguagePreferenceLoadWarning.StorageFailed("[redacted-storage]"),
            ),
            service.loadPreference(),
        )
    }

    @Test
    fun saveDelegatesPreferenceAndReturnsSaved() {
        val store = FakeLanguagePreferenceStore()
        val service = LanguagePreferenceService(store)

        val result = service.savePreference(LanguagePreference.Locale("uk-UA"))

        assertEquals(LanguagePreferenceSaveResult.Saved, result)
        assertEquals(listOf<LanguagePreference>(LanguagePreference.Locale("uk-UA")), store.savedPreferences)
    }

    @Test
    fun saveStorageFailureIsReturnedWithoutPretendingToSave() {
        val store = FakeLanguagePreferenceStore(
            saveResult = LanguagePreferenceSaveResult.StorageFailed("[redacted-save]"),
        )
        val service = LanguagePreferenceService(store)

        val result = service.savePreference(LanguagePreference.System)

        assertEquals(LanguagePreferenceSaveResult.StorageFailed("[redacted-save]"), result)
        assertEquals(listOf<LanguagePreference>(LanguagePreference.System), store.savedPreferences)
    }

    private fun service(loadResult: LanguagePreferenceLoadResult): LanguagePreferenceService =
        LanguagePreferenceService(FakeLanguagePreferenceStore(loadResult = loadResult))
}
