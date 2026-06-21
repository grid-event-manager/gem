package org.gem.core.language

sealed interface LanguagePreference {
    data object System : LanguagePreference

    data class Locale(val localeTag: GemLocaleTag) : LanguagePreference {
        constructor(localeTag: String) : this(GemLocaleTag(localeTag))
    }
}
