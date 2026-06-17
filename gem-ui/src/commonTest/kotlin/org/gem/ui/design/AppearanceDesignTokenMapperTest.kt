package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.core.appearance.AppearanceElementTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceDesignTokenMapperTest {
    @Test
    fun mapsDraftColoursAndFontsIntoDesignTokens() {
        val draft = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.LIGHT).copy(
            textFonts = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.LIGHT).textFonts +
                (AppearanceTextTarget.TITLE_BAR to AppearanceFontFamily("Display")),
            textColors = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.LIGHT).textColors +
                (AppearanceTextTarget.MAIN_BODY to AppearanceColor.require("#112233")) +
                (AppearanceTextTarget.MENU_LABELS to AppearanceColor.require("#445566")),
            elementColors = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.LIGHT).elementColors +
                (AppearanceElementTarget.PAGE_BACKGROUND to AppearanceColor.require("#223344")) +
                (AppearanceElementTarget.HAMBURGER_BARS to AppearanceColor.require("#778899")),
        )

        val tokens = AppearanceDesignTokenMapper.tokens(
            draft = draft,
            availableFonts = listOf(AppearanceFontFamily("Display")),
            platformFontFamilyResolver = PlatformFontFamilyResolver { FontFamily.Serif },
        )

        assertEquals(Color(0xFF223344), tokens.colors.page)
        assertEquals(Color(0xFF112233), tokens.colors.body)
        assertEquals(Color(0xFF445566), tokens.colors.topBarMenuInk)
        assertEquals(Color(0xFF778899), tokens.colors.topBarButtonInk)
        assertEquals(FontFamily.Serif, tokens.typeScale.textTargetFontFamilies[AppearanceTextTarget.TITLE_BAR])
    }

    @Test
    fun keepsBaselineTypeSizesAndWeightsStable() {
        val baseline = GemTypeScale()
        val tokens = AppearanceDesignTokenMapper.tokens(
            draft = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.DARK),
            availableFonts = emptyList(),
            platformFontFamilyResolver = PlatformFontFamilyResolver { FontFamily.Monospace },
        )

        assertEquals(baseline.body.fontSize, tokens.typeScale.body.fontSize)
        assertEquals(baseline.button.lineHeight, tokens.typeScale.button.lineHeight)
        assertEquals(baseline.brandTitle.fontWeight, tokens.typeScale.brandTitle.fontWeight)
    }
}
