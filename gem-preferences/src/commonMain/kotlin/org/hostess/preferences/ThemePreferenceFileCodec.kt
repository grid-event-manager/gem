package org.hostess.preferences

import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceLoadResult

class ThemePreferenceFileCodec {
    fun encode(preference: ThemePreference): String =
        "$THEME_PREFERENCE_KEY=${preference.name}\n"

    fun decode(text: String): ThemePreferenceLoadResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ThemePreferenceLoadResult.InvalidValue(null)
        }

        val lines = trimmed.lines()
        if (lines.size != SINGLE_RECORD_LINE_COUNT) {
            return ThemePreferenceLoadResult.InvalidValue(trimmed)
        }

        val line = lines.single()
        val separatorIndex = line.indexOf(KEY_VALUE_SEPARATOR)
        if (separatorIndex < 0) {
            return ThemePreferenceLoadResult.InvalidValue(line)
        }

        val key = line.substring(0, separatorIndex)
        val rawValue = line.substring(separatorIndex + 1)
        if (key != THEME_PREFERENCE_KEY) {
            return ThemePreferenceLoadResult.InvalidValue(line)
        }
        if (rawValue.isBlank()) {
            return ThemePreferenceLoadResult.InvalidValue(rawValue.ifBlank { "" })
        }

        val preference = ThemePreference.entries.firstOrNull { it.name == rawValue }
            ?: return ThemePreferenceLoadResult.InvalidValue(rawValue)
        return ThemePreferenceLoadResult.Loaded(preference)
    }

    private companion object {
        const val THEME_PREFERENCE_KEY: String = "themePreference"
        const val KEY_VALUE_SEPARATOR: Char = '='
        const val SINGLE_RECORD_LINE_COUNT: Int = 1
    }
}
