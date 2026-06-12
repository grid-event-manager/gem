package org.gem.core.theme

class ThemePreferenceService(
    private val store: ThemePreferenceStore,
) {
    fun loadPreference(): ThemePreferenceSnapshot =
        when (val loaded = store.load()) {
            is ThemePreferenceLoadResult.Loaded -> ThemePreferenceSnapshot(loaded.preference)
            ThemePreferenceLoadResult.Missing -> ThemePreferenceSnapshot(ThemePreference.SYSTEM)
            is ThemePreferenceLoadResult.InvalidValue -> ThemePreferenceSnapshot(
                preference = ThemePreference.SYSTEM,
                warning = ThemePreferenceLoadWarning.InvalidValue(loaded.rawValue),
            )
            is ThemePreferenceLoadResult.StorageFailed -> ThemePreferenceSnapshot(
                preference = ThemePreference.SYSTEM,
                warning = ThemePreferenceLoadWarning.StorageFailed(loaded.message),
            )
        }

    fun savePreference(preference: ThemePreference): ThemePreferenceSaveResult =
        store.save(preference)
}
