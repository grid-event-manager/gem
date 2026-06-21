package org.gem.ui.testing

import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult
import org.gem.core.language.LanguagePreferenceSaveResult
import org.gem.core.language.LanguagePreferenceStore

class FakeLanguagePreferenceStore(
    var loadResult: LanguagePreferenceLoadResult = LanguagePreferenceLoadResult.Missing,
    var saveResult: LanguagePreferenceSaveResult = LanguagePreferenceSaveResult.Saved,
) : LanguagePreferenceStore {
    var lastSavedPreference: LanguagePreference? = null
        private set

    override fun load(): LanguagePreferenceLoadResult =
        loadResult

    override fun save(preference: LanguagePreference): LanguagePreferenceSaveResult {
        lastSavedPreference = preference
        return saveResult
    }
}
