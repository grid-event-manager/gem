package org.hostess.ui.testing

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.core.theme.ThemePreferenceStore

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
