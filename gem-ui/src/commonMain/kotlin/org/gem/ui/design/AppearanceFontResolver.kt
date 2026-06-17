package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily

class AppearanceFontResolver(
    private val availableFamilies: List<AppearanceFontFamily>,
) {
    fun resolve(
        requested: AppearanceFontFamily,
        targetDefault: AppearanceFontFamily,
    ): AppearanceFontFamily =
        exactPlatformMatch(requested)
            ?: exactPlatformMatch(targetDefault)
            ?: availableFamilies.firstOrNull()
            ?: AppearanceFontFamily(SANS_SERIF)

    private fun exactPlatformMatch(family: AppearanceFontFamily): AppearanceFontFamily? =
        availableFamilies.firstOrNull { it.value.equals(family.value, ignoreCase = true) }

    private companion object {
        const val SANS_SERIF = "sans-serif"
    }
}
