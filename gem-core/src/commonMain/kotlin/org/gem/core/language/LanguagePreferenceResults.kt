package org.gem.core.language

sealed interface LanguagePreferenceLoadResult {
    data class Loaded(val preference: LanguagePreference) : LanguagePreferenceLoadResult
    data object Missing : LanguagePreferenceLoadResult
    data class InvalidValue(val rawValue: String?) : LanguagePreferenceLoadResult
    data class StorageFailed(val message: String? = null) : LanguagePreferenceLoadResult
}

sealed interface LanguagePreferenceLoadWarning {
    data class InvalidValue(val rawValue: String?) : LanguagePreferenceLoadWarning
    data class StorageFailed(val message: String? = null) : LanguagePreferenceLoadWarning
}

sealed interface LanguagePreferenceSaveResult {
    data object Saved : LanguagePreferenceSaveResult
    data class StorageFailed(val message: String? = null) : LanguagePreferenceSaveResult
}

data class LanguagePreferenceSnapshot(
    val preference: LanguagePreference,
    val warning: LanguagePreferenceLoadWarning? = null,
)
