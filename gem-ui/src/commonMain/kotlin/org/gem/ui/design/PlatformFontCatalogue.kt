package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily

fun interface PlatformFontCatalogue {
    fun availableFamilies(): List<AppearanceFontFamily>
}

internal object PlatformFontFamilyNames {
    fun normalize(names: Iterable<String>): List<AppearanceFontFamily> =
        names
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })
            .map { AppearanceFontFamily(it) }

    fun normalizeWithSansSerifAlias(names: Iterable<String>): List<AppearanceFontFamily> =
        normalize(names)
            .filterNot { it.value.equals(SANS_SERIF_ALIAS, ignoreCase = true) } +
            AppearanceFontFamily(SANS_SERIF_ALIAS)

    private const val SANS_SERIF_ALIAS = "sans-serif"
}
