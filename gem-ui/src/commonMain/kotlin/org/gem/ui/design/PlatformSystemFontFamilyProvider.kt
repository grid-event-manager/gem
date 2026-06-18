package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily

fun interface PlatformSystemFontFamilyProvider {
    fun defaultFamily(availableFamilies: List<AppearanceFontFamily>): AppearanceFontFamily
}

internal object PlatformSystemFontFamilySelection {
    val sansSerif = AppearanceFontFamily("sans-serif")

    fun select(
        availableFamilies: List<AppearanceFontFamily>,
        candidates: List<String>,
    ): AppearanceFontFamily {
        val normalized = PlatformFontFamilyNames.normalize(availableFamilies.map { it.value })
        return candidates
            .firstNotNullOfOrNull { candidate ->
                normalized.firstOrNull { it.value.equals(candidate, ignoreCase = true) }
            }
            ?: sansSerif
    }
}
