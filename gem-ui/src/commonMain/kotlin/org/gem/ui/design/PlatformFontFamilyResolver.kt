package org.gem.ui.design

import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceFontFamily

fun interface PlatformFontFamilyResolver {
    fun resolve(family: AppearanceFontFamily): FontFamily
}
