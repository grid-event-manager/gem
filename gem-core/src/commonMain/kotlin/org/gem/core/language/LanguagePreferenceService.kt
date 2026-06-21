package org.gem.core.language

class LanguagePreferenceService(
    private val store: LanguagePreferenceStore,
) {
    fun loadPreference(): LanguagePreferenceSnapshot =
        when (val loaded = store.load()) {
            is LanguagePreferenceLoadResult.Loaded -> LanguagePreferenceSnapshot(loaded.preference)
            LanguagePreferenceLoadResult.Missing -> LanguagePreferenceSnapshot(LanguagePreference.System)
            is LanguagePreferenceLoadResult.InvalidValue -> LanguagePreferenceSnapshot(
                preference = LanguagePreference.System,
                warning = LanguagePreferenceLoadWarning.InvalidValue(loaded.rawValue),
            )
            is LanguagePreferenceLoadResult.StorageFailed -> LanguagePreferenceSnapshot(
                preference = LanguagePreference.System,
                warning = LanguagePreferenceLoadWarning.StorageFailed(loaded.message),
            )
        }

    fun savePreference(preference: LanguagePreference): LanguagePreferenceSaveResult =
        store.save(preference)
}
