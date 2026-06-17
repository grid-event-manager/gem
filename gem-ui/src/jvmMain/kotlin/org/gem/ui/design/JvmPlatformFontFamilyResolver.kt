package org.gem.ui.design

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceFontFamily

class JvmPlatformFontFamilyResolver : PlatformFontFamilyResolver {
    @OptIn(ExperimentalTextApi::class)
    override fun resolve(family: AppearanceFontFamily): FontFamily =
        FontFamily(family.value)
}
