package org.gem.ui.design

import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import android.os.Build
import java.io.File
import org.gem.core.appearance.AppearanceFontFamily

class AndroidPlatformFontCatalogue(
    private val sdkInt: () -> Int = { Build.VERSION.SDK_INT },
    private val availableFontNames: () -> List<String> = ::systemFontNames,
) : PlatformFontCatalogue {
    override fun availableFamilies(): List<AppearanceFontFamily> =
        if (sdkInt() >= SYSTEM_FONT_API_LEVEL) {
            PlatformFontFamilyNames.normalize(availableFontNames())
        } else {
            PlatformFontFamilyNames.normalize(FIXED_API_26_TO_28_FAMILIES)
        }

    private companion object {
        const val SYSTEM_FONT_API_LEVEL = 29

        val FIXED_API_26_TO_28_FAMILIES = listOf(
            "sans",
            "serif",
            "monospace",
            "casual",
            "cursive",
            "sans-serif",
            "sans-serif-condensed",
            "sans-serif-light",
            "sans-serif-medium",
            "sans-serif-black",
            "serif-monospace",
        )

        private fun systemFontNames(): List<String> =
            SystemFonts.getAvailableFonts()
                .mapNotNull(Font::getFile)
                .map { it.getName() }
                .map { it.substringBeforeLast('.', missingDelimiterValue = it) }
    }
}
