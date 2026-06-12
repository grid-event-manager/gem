package org.hostess.core.testing

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.core.theme.ThemePreferenceStore

class FakeThemePreferenceStore(
    var loadResult: ThemePreferenceLoadResult = ThemePreferenceLoadResult.Missing,
    var saveResult: ThemePreferenceSaveResult = ThemePreferenceSaveResult.Saved,
) : ThemePreferenceStore {
    val savedPreferences = mutableListOf<ThemePreference>()

    override fun load(): ThemePreferenceLoadResult =
        loadResult

    override fun save(preference: ThemePreference): ThemePreferenceSaveResult {
        savedPreferences += preference
        return saveResult
    }
}
