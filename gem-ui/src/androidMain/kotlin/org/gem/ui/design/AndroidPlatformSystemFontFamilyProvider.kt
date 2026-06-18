package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily

object AndroidPlatformSystemFontFamilyProvider : PlatformSystemFontFamilyProvider {
    override fun defaultFamily(availableFamilies: List<AppearanceFontFamily>): AppearanceFontFamily =
        PlatformSystemFontFamilySelection.sansSerif
}
