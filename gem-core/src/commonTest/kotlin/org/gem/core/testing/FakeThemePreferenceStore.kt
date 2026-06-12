package org.gem.core.testing

import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.core.theme.ThemePreferenceStore

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
