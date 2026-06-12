package org.gem.ui.testing

import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceLoadResult
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.core.theme.ThemePreferenceStore

class FakeThemePreferenceStore(
    var loadResult: ThemePreferenceLoadResult = ThemePreferenceLoadResult.Missing,
    var saveResult: ThemePreferenceSaveResult = ThemePreferenceSaveResult.Saved,
) : ThemePreferenceStore {
    var lastSavedPreference: ThemePreference? = null
        private set

    override fun load(): ThemePreferenceLoadResult =
        loadResult

    override fun save(preference: ThemePreference): ThemePreferenceSaveResult {
        lastSavedPreference = preference
        return saveResult
    }
}
