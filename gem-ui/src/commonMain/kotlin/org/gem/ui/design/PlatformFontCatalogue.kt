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
}
