package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceTextTarget

object GemSystemThemeProfileFactory {
    fun profile(
        mode: AppearanceMode,
        availableFontFamilies: List<AppearanceFontFamily>,
        platformSystemFontFamilyProvider: PlatformSystemFontFamilyProvider,
    ): AppearanceProfile {
        val family = platformSystemFontFamilyProvider.defaultFamily(availableFontFamilies)
        val textFonts = AppearanceTextTarget.entries.associateWith { family }
        return AppearanceProfileCatalogue.systemProfile(mode, textFonts)
    }
}
