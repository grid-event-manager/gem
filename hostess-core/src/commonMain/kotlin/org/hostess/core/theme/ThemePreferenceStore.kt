package org.hostess.core.theme

interface ThemePreferenceStore {
    fun load(): ThemePreferenceLoadResult

    fun save(preference: ThemePreference): ThemePreferenceSaveResult
}
