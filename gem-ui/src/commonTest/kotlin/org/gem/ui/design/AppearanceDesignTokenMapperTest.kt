package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceTextTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceDesignTokenMapperTest {
    @Test
    fun mapsDraftColoursAndFontsIntoDesignTokens() {
        val base = systemDraft(AppearanceMode.LIGHT)
        val draft = base.copy(
            textFonts = base.textFonts +
                (AppearanceTextTarget.TITLE_BAR to AppearanceFontFamily("Display")),
            textColors = base.textColors +
                (AppearanceTextTarget.MAIN_BODY to AppearanceColor.require("#112233")) +
                (AppearanceTextTarget.MENU_LABELS to AppearanceColor.require("#445566")) +
                (AppearanceTextTarget.THEME_TOGGLE_LABELS to AppearanceColor.require("#556677")),
            elementColors = base.elementColors +
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
        assertEquals(Color(0xFF556677), tokens.colors.themeToggleLabelInk)
        assertEquals(Color(0xFF778899), tokens.colors.topBarButtonInk)
        assertEquals(FontFamily.Serif, tokens.typeScale.textTargetFontFamilies[AppearanceTextTarget.TITLE_BAR])
    }

    @Test
    fun systemDraftsReproducePaletteLedgerDirectAndDerivedValues() {
        val light = tokensFor(systemDraft(AppearanceMode.LIGHT)).colors
        val dark = tokensFor(systemDraft(AppearanceMode.DARK)).colors

        assertEquals(Color(0xFF8AB4C4), light.secondary)
        assertEquals(Color(0xFFB5544D), light.danger)
        assertEquals(Color(0xFF4A7A8A), light.successInk)
        assertEquals(Color(0xFFB8B8B8), light.menuDisabledInk)
        assertEquals(Color(0xFF8B0101), light.interactiveHoverInk)
        assertEquals(Color(0xFF8AB4C4), light.themeToggleLabelInk)
        assertEquals(Color(0xFFE0E0E0), light.disabledBackground)
        assertEquals(Color(0xFF777777), light.disabledInk)
        assertEquals(Color(0xFF4A7A8A), light.selectedInk)
        assertEquals(Color(0xFF8AB4C4), light.topBarInk)

        assertEquals(Color(0xFF8AB4C4), dark.secondary)
        assertEquals(Color(0xFFB5544D), dark.danger)
        assertEquals(Color(0xFF8AB4C4), dark.successInk)
        assertEquals(Color(0xFFA0B0BC), dark.menuDisabledInk)
        assertEquals(Color(0xFFC0C8D0), dark.interactiveHoverInk)
        assertEquals(Color(0xFF8AB4C4), dark.themeToggleLabelInk)
        assertEquals(Color(0xFF2E3F4E), dark.disabledBackground)
        assertEquals(Color(0xFFA0B0BC), dark.disabledInk)
        assertEquals(Color(0xFF8AB4C4), dark.selectedInk)
        assertEquals(Color(0xFF8AB4C4), dark.topBarInk)
    }

    @Test
    fun stockAndCustomDraftsUseSameOrdinaryMapperRoute() {
        val stock = AppearanceDraft.fromProfile(
            AppearanceProfileCatalogue.stockProfiles().first { it.mode == AppearanceMode.DARK },
        )
        val custom = stock.copy(
            elementColors = stock.elementColors +
                (AppearanceElementTarget.ACCENT_TEXT to AppearanceColor.require("#ABCDEF")),
        )

        val stockTokens = tokensFor(stock)
        val customTokens = tokensFor(custom)

        assertEquals(Color(0xFFFF9EDB), stockTokens.colors.secondary)
        assertEquals(Color(0xFFABCDEF), customTokens.colors.secondary)
    }

    @Test
    fun stockDraftSemanticColoursReachVisibleConsumerTokens() {
        val stock = AppearanceDraft.fromProfile(
            AppearanceProfileCatalogue.stockProfiles().first { it.id.value == "stock-princess-dark" },
        )

        val colors = tokensFor(stock).colors

        assertEquals(Color(0xFFBFE9FF), colors.navigationInk)
        assertEquals(Color(0xFFBFE9FF), colors.interactiveHoverInk)
        assertEquals(Color(0xFFBFE9FF), colors.themeToggleLabelInk)
        assertEquals(Color(0xFFACC3D8), colors.muted)
        assertEquals(Color(0xFFFF9EDB), colors.secondary)
        assertEquals(Color(0xFFB5544D), colors.danger)
        assertEquals(Color(0xFFFF9EDB), colors.successInk)
        assertEquals(Color(0xFFACC3D8), colors.menuDisabledInk)
        assertEquals(Color(0xFFFF9EDB), colors.topBarMenuInk)
        assertEquals(Color(0xFFFF9EDB), colors.buttonLabelInk)
    }

    @Test
    fun customDraftChangesFlowThroughDirectAndDerivedColourMapping() {
        val base = systemDraft(AppearanceMode.LIGHT)
        val draft = base.copy(
            textColors = base.textColors +
                (AppearanceTextTarget.MAIN_BODY to AppearanceColor.require("#111111")) +
                (AppearanceTextTarget.FIELD_TEXT to AppearanceColor.require("#101010")) +
                (AppearanceTextTarget.BACK_BUTTON to AppearanceColor.require("#222222")) +
                (AppearanceTextTarget.THEME_TOGGLE_LABELS to AppearanceColor.require("#333333")) +
                (AppearanceTextTarget.SMALL_LABELS to AppearanceColor.require("#444444")),
            elementColors = base.elementColors +
                (AppearanceElementTarget.ACCENT_TEXT to AppearanceColor.require("#EEEEEE")) +
                (AppearanceElementTarget.ERROR_TEXT to AppearanceColor.require("#661111")) +
                (AppearanceElementTarget.STATUS_TEXT to AppearanceColor.require("#116622")) +
                (AppearanceElementTarget.MENU_DISABLED_TEXT to AppearanceColor.require("#777777")) +
                (AppearanceElementTarget.INTERACTIVE_HOVER_TEXT to AppearanceColor.require("#882222")) +
                (AppearanceElementTarget.RULES_AND_SEPARATORS to AppearanceColor.require("#333333")) +
                (AppearanceElementTarget.SELECTED_ITEM_FILL to AppearanceColor.require("#FFFFFF")) +
                (AppearanceElementTarget.PRIMARY_BUTTON_FILL to AppearanceColor.require("#000000")),
        )

        val colors = tokensFor(draft).colors

        assertEquals(Color(0xFF111111), colors.body)
        assertEquals(Color(0xFF101010), colors.ink)
        assertEquals(Color(0xFF222222), colors.navigationInk)
        assertEquals(Color(0xFF333333), colors.themeToggleLabelInk)
        assertEquals(Color(0xFFEEEEEE), colors.secondary)
        assertEquals(Color(0xFF661111), colors.danger)
        assertEquals(Color(0xFF116622), colors.successInk)
        assertEquals(Color(0xFF777777), colors.menuDisabledInk)
        assertEquals(Color(0xFF882222), colors.interactiveHoverInk)
        assertEquals(Color(0xFF333333), colors.disabledBackground)
        assertEquals(Color(0xFF444444), colors.disabledInk)
        assertEquals(Color(0xFF000000), colors.selectedInk)
    }

    @Test
    fun themeToggleLabelColourDoesNotAliasButtonLabels() {
        val base = systemDraft(AppearanceMode.LIGHT)
        val baseline = tokensFor(base).colors
        val buttonOnly = base.copy(
            textColors = base.textColors +
                (AppearanceTextTarget.BUTTON_LABELS to AppearanceColor.require("#AA0000")),
        )
        val toggleOnly = base.copy(
            textColors = base.textColors +
                (AppearanceTextTarget.THEME_TOGGLE_LABELS to AppearanceColor.require("#00AA00")),
        )

        val buttonColors = tokensFor(buttonOnly).colors
        val toggleColors = tokensFor(toggleOnly).colors

        assertEquals(Color(0xFFAA0000), buttonColors.buttonLabelInk)
        assertEquals(Color(0xFFAA0000), buttonColors.primaryInk)
        assertEquals(baseline.themeToggleLabelInk, buttonColors.themeToggleLabelInk)
        assertEquals(Color(0xFF00AA00), toggleColors.themeToggleLabelInk)
        assertEquals(baseline.buttonLabelInk, toggleColors.buttonLabelInk)
    }

    @Test
    fun renderedTextTargetsMapToIndependentTypeScaleSlots() {
        val base = systemDraft(AppearanceMode.LIGHT)
        val default = AppearanceFontFamily("sans-serif")
        val title = AppearanceFontFamily("TitleFont")
        val field = AppearanceFontFamily("FieldFont")
        val back = AppearanceFontFamily("BackFont")
        val toggle = AppearanceFontFamily("ToggleFont")
        val button = AppearanceFontFamily("ButtonFont")
        val small = AppearanceFontFamily("SmallFont")
        val menu = AppearanceFontFamily("MenuFont")
        val draft = base.copy(
            textFonts = base.textFonts +
                (AppearanceTextTarget.TITLE_BAR to title) +
                (AppearanceTextTarget.FIELD_TEXT to field) +
                (AppearanceTextTarget.BACK_BUTTON to back) +
                (AppearanceTextTarget.THEME_TOGGLE_LABELS to toggle) +
                (AppearanceTextTarget.BUTTON_LABELS to button) +
                (AppearanceTextTarget.SMALL_LABELS to small) +
                (AppearanceTextTarget.MENU_LABELS to menu),
        )

        val tokens = AppearanceDesignTokenMapper.tokens(
            draft = draft,
            availableFonts = listOf(default, title, field, back, toggle, button, small, menu),
            platformFontFamilyResolver = PlatformFontFamilyResolver { family ->
                when (family) {
                    default -> FontFamily.SansSerif
                    title -> FontFamily.Serif
                    field -> FontFamily.Monospace
                    back -> FontFamily.Cursive
                    toggle -> FontFamily.Monospace
                    button -> FontFamily.Serif
                    small -> FontFamily.Default
                    menu -> FontFamily.Cursive
                    else -> FontFamily.SansSerif
                }
            },
        )

        assertEquals(FontFamily.SansSerif, tokens.typeScale.body.fontFamily)
        assertEquals(FontFamily.Serif, tokens.typeScale.brandTitle.fontFamily)
        assertEquals(FontFamily.Monospace, tokens.typeScale.fieldText.fontFamily)
        assertEquals(FontFamily.Cursive, tokens.typeScale.backLabel.fontFamily)
        assertEquals(FontFamily.Monospace, tokens.typeScale.themeToggleLabel.fontFamily)
        assertEquals(FontFamily.Serif, tokens.typeScale.button.fontFamily)
        assertEquals(FontFamily.Default, tokens.typeScale.smallLabel.fontFamily)
        assertEquals(FontFamily.Default, tokens.typeScale.statusPill.fontFamily)
        assertEquals(FontFamily.Cursive, tokens.typeScale.menuItem.fontFamily)
    }

    @Test
    fun keepsBaselineTypeSizesAndWeightsStable() {
        val baseline = GemTypeScale()
        val tokens = AppearanceDesignTokenMapper.tokens(
            draft = systemDraft(AppearanceMode.DARK),
            availableFonts = emptyList(),
            platformFontFamilyResolver = PlatformFontFamilyResolver { FontFamily.Monospace },
        )

        assertEquals(baseline.body.fontSize, tokens.typeScale.body.fontSize)
        assertEquals(baseline.button.lineHeight, tokens.typeScale.button.lineHeight)
        assertEquals(baseline.brandTitle.fontWeight, tokens.typeScale.brandTitle.fontWeight)
    }

    private fun systemDraft(mode: AppearanceMode): AppearanceDraft =
        AppearanceDraft.fromProfile(
            AppearanceProfileCatalogue.systemProfile(
                mode = mode,
                textFonts = AppearanceTextTarget.entries.associateWith {
                    AppearanceFontFamily("sans-serif")
                },
            ),
        ).copy(selectedProfileId = null)

    private fun tokensFor(draft: AppearanceDraft): GemDesignTokens =
        AppearanceDesignTokenMapper.tokens(
            draft = draft,
            availableFonts = emptyList(),
            platformFontFamilyResolver = PlatformFontFamilyResolver { FontFamily.SansSerif },
        )
}
