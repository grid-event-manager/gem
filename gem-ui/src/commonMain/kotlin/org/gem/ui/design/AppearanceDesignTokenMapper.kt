package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceTextTarget
import kotlin.math.pow

object AppearanceDesignTokenMapper {
    fun tokens(
        draft: AppearanceDraft,
        availableFonts: List<AppearanceFontFamily>,
        platformFontFamilyResolver: PlatformFontFamilyResolver,
    ): GemDesignTokens {
        val baseline = GemDesignTokens()
        val fontResolver = AppearanceFontResolver(availableFonts)
        val resolvedFonts = AppearanceTargetCatalogue.textTargets.associate { spec ->
            val requested = draft.textFonts.getValue(spec.target)
            val resolved = fontResolver.resolve(requested, spec.defaultFontFamily)
            spec.target to platformFontFamilyResolver.resolve(resolved)
        }

        return baseline.copy(
            colors = colorsFrom(
                baseline = baseline.colors,
                mode = draft.mode,
                textColors = draft.textColors,
                elementColors = draft.elementColors,
            ),
            typeScale = typeScaleFrom(
                baseline = baseline.typeScale,
                fontFamilies = resolvedFonts,
            ),
        )
    }

    private fun colorsFrom(
        baseline: GemColors,
        mode: AppearanceMode,
        textColors: Map<AppearanceTextTarget, AppearanceColor>,
        elementColors: Map<AppearanceElementTarget, AppearanceColor>,
    ): GemColors {
        var colors = baseline

        AppearanceTargetCatalogue.elementTargets.forEach { spec ->
            colors = colors.withElementColor(spec.colorSlot, elementColors.getValue(spec.target).toComposeColor())
        }
        AppearanceTargetCatalogue.textTargets.forEach { spec ->
            colors = colors.withTextColor(spec.colorSlot, textColors.getValue(spec.target).toComposeColor())
        }

        return colors.withDerivedStateColors(mode)
    }

    private fun typeScaleFrom(
        baseline: GemTypeScale,
        fontFamilies: Map<AppearanceTextTarget, FontFamily>,
    ): GemTypeScale {
        val targetFonts = AppearanceTextTarget.entries.associateWith { target ->
            fontFamilies.getValue(target)
        }
        return baseline
            .copy(textTargetFontFamilies = targetFonts)
            .copy(
                brandTitle = baseline.brandTitle.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.TITLE_BAR),
                ),
                brandSubtitle = baseline.brandSubtitle.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.TITLE_SUBTITLE),
                ),
                sectionTitle = baseline.sectionTitle.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.SECTION_HEADINGS),
                ),
                body = baseline.body.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.MAIN_BODY),
                ),
                smallLabel = baseline.smallLabel.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.SMALL_LABELS),
                ),
                button = baseline.button.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.BUTTON_LABELS),
                ),
                menuItem = baseline.menuItem.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.MENU_LABELS),
                ),
                statusPill = baseline.statusPill.copy(
                    fontFamily = fontFamilies.getValue(AppearanceTextTarget.SMALL_LABELS),
                ),
            )
    }

    private fun GemColors.withTextColor(
        slot: AppearanceTextColorSlot,
        color: Color,
    ): GemColors =
        when (slot) {
            AppearanceTextColorSlot.BRAND_TITLE -> copy(brandInitialsInk = color)
            AppearanceTextColorSlot.BRAND_SUBTITLE -> copy(brandWordmark = color)
            AppearanceTextColorSlot.BRAND_LOGO -> copy(brandMark = color, brandAccent = color)
            AppearanceTextColorSlot.SECTION -> copy(ink = color)
            AppearanceTextColorSlot.BODY -> copy(body = color)
            AppearanceTextColorSlot.FIELD_TEXT -> copy(ink = color)
            AppearanceTextColorSlot.SMALL_LABEL -> copy(muted = color)
            AppearanceTextColorSlot.BUTTON_LABEL -> copy(buttonLabelInk = color, primaryInk = color)
            AppearanceTextColorSlot.MENU_LABEL -> copy(topBarMenuInk = color)
            AppearanceTextColorSlot.CLOCK -> copy(topBarClockInk = color)
            AppearanceTextColorSlot.BACK -> copy(navigationInk = color)
            AppearanceTextColorSlot.THEME_TOGGLE -> this
        }

    private fun GemColors.withElementColor(
        slot: AppearanceElementColorSlot,
        color: Color,
    ): GemColors =
        when (slot) {
            AppearanceElementColorSlot.PAGE -> copy(page = color)
            AppearanceElementColorSlot.CARD -> copy(surfaceStrong = color)
            AppearanceElementColorSlot.PANEL -> copy(surface = color)
            AppearanceElementColorSlot.FIELD -> copy(fieldSurface = color)
            AppearanceElementColorSlot.FIELD_BORDER -> copy(fieldBorder = color, lineStrong = color, toggleBorder = color)
            AppearanceElementColorSlot.TITLE_BAR -> copy(topBar = color)
            AppearanceElementColorSlot.TITLE_BUTTON -> copy(topBarButtonSurface = color)
            AppearanceElementColorSlot.TITLE_BUTTON_BORDER -> copy(topBarButtonBorder = color)
            AppearanceElementColorSlot.HAMBURGER_BACKGROUND -> copy(topBarButtonSurface = color)
            AppearanceElementColorSlot.HAMBURGER_BORDER -> copy(topBarButtonBorder = color)
            AppearanceElementColorSlot.HAMBURGER_BARS -> copy(topBarButtonInk = color)
            AppearanceElementColorSlot.TOGGLE_TRACK -> copy(toggleTrack = color)
            AppearanceElementColorSlot.TOGGLE_TRACK_SELECTED -> copy(toggleTrackSelected = color)
            AppearanceElementColorSlot.TOGGLE_KNOB -> copy(toggleKnob = color)
            AppearanceElementColorSlot.ACCENT_TEXT -> copy(secondary = color)
            AppearanceElementColorSlot.ERROR_TEXT -> copy(danger = color)
            AppearanceElementColorSlot.STATUS_TEXT -> copy(successInk = color)
            AppearanceElementColorSlot.MENU_DISABLED_TEXT -> copy(menuDisabledInk = color)
            AppearanceElementColorSlot.INTERACTIVE_HOVER_TEXT -> copy(interactiveHoverInk = color)
            AppearanceElementColorSlot.PRIMARY -> copy(primary = color)
            AppearanceElementColorSlot.SELECTED -> copy(selectedBackground = color, menuActive = color)
            AppearanceElementColorSlot.MENU -> copy(menuSurface = color)
            AppearanceElementColorSlot.MENU_HOVER -> copy(menuHover = color)
            AppearanceElementColorSlot.STATUS -> copy(statusBackground = color, successBackground = color)
            AppearanceElementColorSlot.SEPARATOR -> copy(line = color, shellBorder = color)
        }

    private fun GemColors.withDerivedStateColors(
        @Suppress("UNUSED_PARAMETER")
        mode: AppearanceMode,
    ): GemColors =
        copy(
            disabledBackground = line,
            disabledInk = muted,
            selectedInk = readableAccentOn(
                selectedBackground = selectedBackground,
                preferred = secondary,
                fallback = primary,
            ),
            topBarInk = secondary,
        )

    private fun readableAccentOn(
        selectedBackground: Color,
        preferred: Color,
        fallback: Color,
    ): Color =
        if (contrastRatio(preferred, selectedBackground) >= MINIMUM_READABLE_CONTRAST) {
            preferred
        } else {
            fallback
        }

    private fun contrastRatio(
        foreground: Color,
        background: Color,
    ): Double {
        val foregroundLuminance = relativeLuminance(foreground)
        val backgroundLuminance = relativeLuminance(background)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private fun relativeLuminance(color: Color): Double =
        LUMINANCE_RED * color.red.linearized() +
            LUMINANCE_GREEN * color.green.linearized() +
            LUMINANCE_BLUE * color.blue.linearized()

    private fun Float.linearized(): Double {
        val component = toDouble()
        return if (component <= SRGB_LOW_COMPONENT) {
            component / SRGB_LOW_DIVISOR
        } else {
            ((component + SRGB_OFFSET) / SRGB_DIVISOR).pow(SRGB_EXPONENT)
        }
    }

    private const val MINIMUM_READABLE_CONTRAST = 4.5
    private const val LUMINANCE_OFFSET = 0.05
    private const val LUMINANCE_RED = 0.2126
    private const val LUMINANCE_GREEN = 0.7152
    private const val LUMINANCE_BLUE = 0.0722
    private const val SRGB_LOW_COMPONENT = 0.04045
    private const val SRGB_LOW_DIVISOR = 12.92
    private const val SRGB_OFFSET = 0.055
    private const val SRGB_DIVISOR = 1.055
    private const val SRGB_EXPONENT = 2.4
}
