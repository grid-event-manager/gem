package org.gem.core.testing

import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult
import org.gem.core.language.LanguagePreferenceSaveResult
import org.gem.core.language.LanguagePreferenceStore

class FakeLanguagePreferenceStore(
    var loadResult: LanguagePreferenceLoadResult = LanguagePreferenceLoadResult.Missing,
    var saveResult: LanguagePreferenceSaveResult = LanguagePreferenceSaveResult.Saved,
) : LanguagePreferenceStore {
    val savedPreferences = mutableListOf<LanguagePreference>()

    override fun load(): LanguagePreferenceLoadResult =
        loadResult

    override fun save(preference: LanguagePreference): LanguagePreferenceSaveResult {
        savedPreferences += preference
        return saveResult
    }
}
