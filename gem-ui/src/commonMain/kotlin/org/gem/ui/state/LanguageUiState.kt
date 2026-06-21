package org.gem.ui.state

import org.gem.core.language.LanguagePreference
import org.gem.ui.text.GemTextKey

data class LanguageUiState(
    val expanded: Boolean = false,
    val preference: LanguagePreference = LanguagePreference.System,
    val requestedLocaleTag: String = "",
    val resolvedLocaleTag: String = "",
    val warningKey: GemTextKey? = null,
    val options: List<LanguageOption> = emptyList(),
)

sealed interface LanguageOption {
    val preference: LanguagePreference?

    data object Placeholder : LanguageOption {
        override val preference: LanguagePreference? = null
    }

    data object System : LanguageOption {
        override val preference: LanguagePreference = LanguagePreference.System
    }

    data class Locale(
        val localeTag: String,
        val nativeName: String,
    ) : LanguageOption {
        override val preference: LanguagePreference = LanguagePreference.Locale(localeTag)
    }
}
