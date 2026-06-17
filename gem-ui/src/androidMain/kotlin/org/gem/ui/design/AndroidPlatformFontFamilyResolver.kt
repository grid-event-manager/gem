package org.gem.ui.design

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceFontFamily

class AndroidPlatformFontFamilyResolver : PlatformFontFamilyResolver {
    override fun resolve(family: AppearanceFontFamily): FontFamily =
        FontFamily(Typeface.create(family.value, Typeface.NORMAL))
}
