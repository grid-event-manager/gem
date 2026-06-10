package org.hostess.core.theme

sealed interface ThemePreferenceLoadResult {
    data class Loaded(val preference: ThemePreference) : ThemePreferenceLoadResult
    data object Missing : ThemePreferenceLoadResult
    data class InvalidValue(val rawValue: String?) : ThemePreferenceLoadResult
    data class StorageFailed(val message: String? = null) : ThemePreferenceLoadResult
}

sealed interface ThemePreferenceLoadWarning {
    data class InvalidValue(val rawValue: String?) : ThemePreferenceLoadWarning
    data class StorageFailed(val message: String? = null) : ThemePreferenceLoadWarning
}

sealed interface ThemePreferenceSaveResult {
    data object Saved : ThemePreferenceSaveResult
    data class StorageFailed(val message: String? = null) : ThemePreferenceSaveResult
}

data class ThemePreferenceSnapshot(
    val preference: ThemePreference,
    val warning: ThemePreferenceLoadWarning? = null,
)
