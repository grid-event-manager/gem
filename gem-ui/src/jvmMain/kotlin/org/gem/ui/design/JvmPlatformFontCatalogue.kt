package org.gem.ui.design

import java.awt.GraphicsEnvironment
import org.gem.core.appearance.AppearanceFontFamily

class JvmPlatformFontCatalogue(
    private val fontFamilyNames: () -> Array<String> = {
        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
    },
) : PlatformFontCatalogue {
    override fun availableFamilies(): List<AppearanceFontFamily> =
        PlatformFontFamilyNames.normalizeWithSansSerifAlias(fontFamilyNames().asIterable())
}
