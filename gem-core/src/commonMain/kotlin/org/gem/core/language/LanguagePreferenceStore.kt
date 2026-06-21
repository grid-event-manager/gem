package org.gem.core.language

interface LanguagePreferenceStore {
    fun load(): LanguagePreferenceLoadResult

    fun save(preference: LanguagePreference): LanguagePreferenceSaveResult
}
