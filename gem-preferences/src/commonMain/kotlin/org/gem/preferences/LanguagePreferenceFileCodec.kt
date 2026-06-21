package org.gem.preferences

import org.gem.core.language.LanguagePreference
import org.gem.core.language.LanguagePreferenceLoadResult

class LanguagePreferenceFileCodec {
    fun encode(preference: LanguagePreference): String =
        "$LANGUAGE_PREFERENCE_KEY=${preference.encodedValue()}\n"

    fun decode(text: String): LanguagePreferenceLoadResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return LanguagePreferenceLoadResult.InvalidValue(null)
        }

        val lines = trimmed.lines()
        if (lines.size != SINGLE_RECORD_LINE_COUNT) {
            return LanguagePreferenceLoadResult.InvalidValue(trimmed)
        }

        val line = lines.single()
        val separatorIndex = line.indexOf(KEY_VALUE_SEPARATOR)
        if (separatorIndex < 0) {
            return LanguagePreferenceLoadResult.InvalidValue(line)
        }

        val key = line.substring(0, separatorIndex)
        val rawValue = line.substring(separatorIndex + 1)
        if (key != LANGUAGE_PREFERENCE_KEY) {
            return LanguagePreferenceLoadResult.InvalidValue(line)
        }
        if (rawValue.isBlank()) {
            return LanguagePreferenceLoadResult.InvalidValue(rawValue.ifBlank { "" })
        }

        return when {
            rawValue == SYSTEM_VALUE -> LanguagePreferenceLoadResult.Loaded(LanguagePreference.System)
            rawValue.startsWith(LOCALE_PREFIX) -> decodeLocale(rawValue)
            else -> LanguagePreferenceLoadResult.InvalidValue(rawValue)
        }
    }

    private fun decodeLocale(rawValue: String): LanguagePreferenceLoadResult {
        val localeTag = rawValue.removePrefix(LOCALE_PREFIX)
        return if (localeTag.isBlank()) {
            LanguagePreferenceLoadResult.InvalidValue(rawValue)
        } else {
            LanguagePreferenceLoadResult.Loaded(LanguagePreference.Locale(localeTag))
        }
    }

    private fun LanguagePreference.encodedValue(): String =
        when (this) {
            is LanguagePreference.Locale -> "$LOCALE_PREFIX${localeTag.value}"
            LanguagePreference.System -> SYSTEM_VALUE
        }

    private companion object {
        const val LANGUAGE_PREFERENCE_KEY: String = "languagePreference"
        const val KEY_VALUE_SEPARATOR: Char = '='
        const val SINGLE_RECORD_LINE_COUNT: Int = 1
        const val SYSTEM_VALUE: String = "SYSTEM"
        const val LOCALE_PREFIX: String = "LOCALE:"
    }
}
