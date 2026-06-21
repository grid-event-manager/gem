package org.gem.core.language

sealed interface LanguagePreference {
    data object System : LanguagePreference

    data class Locale(val localeTag: String) : LanguagePreference {
        init {
            require(localeTag.isNotBlank()) { "localeTag must not be blank" }
        }
    }
}
