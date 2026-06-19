package org.gem.core.appearance

object AppearanceProfileCompletion {
    fun completeTextFonts(
        partial: Map<AppearanceTextTarget, AppearanceFontFamily>,
    ): Map<AppearanceTextTarget, AppearanceFontFamily> =
        completeTargets(
            targets = AppearanceTextTarget.entries,
            partial = partial,
            fallback = { AppearanceFontFamily(STOCK_CUSTOM_FALLBACK_FONT) },
        )

    fun completeTextColors(
        mode: AppearanceMode,
        partial: Map<AppearanceTextTarget, AppearanceColor>,
    ): Map<AppearanceTextTarget, AppearanceColor> {
        val fallbackMap = signedOffTextColors(mode)
        return completeTargets(
            targets = AppearanceTextTarget.entries,
            partial = partial,
            fallback = fallbackMap::getValue,
        )
    }

    fun completeElementColors(
        mode: AppearanceMode,
        partial: Map<AppearanceElementTarget, AppearanceColor>,
    ): Map<AppearanceElementTarget, AppearanceColor> {
        val fallbackMap = signedOffElementColors(mode)
        return completeTargets(
            targets = AppearanceElementTarget.entries,
            partial = partial,
            fallback = fallbackMap::getValue,
        )
    }

    private fun signedOffTextColors(mode: AppearanceMode): Map<AppearanceTextTarget, AppearanceColor> =
        mapOf(
            AppearanceTextTarget.TITLE_BAR to color("#ffffff"),
            AppearanceTextTarget.TITLE_SUBTITLE to color("#a9d6e6"),
            AppearanceTextTarget.LOGO to color("#4a7a8a"),
            AppearanceTextTarget.SECTION_HEADINGS to color("#8ab4c4"),
            AppearanceTextTarget.MAIN_BODY to color(mode.pick(light = "#444444", dark = "#c0c8d0")),
            AppearanceTextTarget.FIELD_TEXT to color(mode.pick(light = "#444444", dark = "#c0c8d0")),
            AppearanceTextTarget.SMALL_LABELS to color(mode.pick(light = "#777777", dark = "#a0b0bc")),
            AppearanceTextTarget.BUTTON_LABELS to color(mode.pick(light = "#5a778c", dark = "#8ab4c4")),
            AppearanceTextTarget.MENU_LABELS to color(mode.pick(light = "#5a778c", dark = "#8ab4c4")),
            AppearanceTextTarget.SLT_CLOCK to color("#ffffff"),
            AppearanceTextTarget.BACK_BUTTON to color("#8ab4c4"),
            AppearanceTextTarget.THEME_TOGGLE_LABELS to color("#8ab4c4"),
        )

    private fun signedOffElementColors(mode: AppearanceMode): Map<AppearanceElementTarget, AppearanceColor> =
        mapOf(
            AppearanceElementTarget.PAGE_BACKGROUND to color(mode.pick(light = "#ffffff", dark = "#2a3441")),
            AppearanceElementTarget.CARD_BACKGROUND to color(mode.pick(light = "#ffffff", dark = "#2f3d4a")),
            AppearanceElementTarget.PANEL_BACKGROUND to color(mode.pick(light = "#fafaf8", dark = "#243039")),
            AppearanceElementTarget.FIELD_BACKGROUND to color(mode.pick(light = "#ffffff", dark = "#2a3441")),
            AppearanceElementTarget.FIELD_CONTROL_BORDERS to color(mode.pick(light = "#8ab4c4", dark = "#3a4d5e")),
            AppearanceElementTarget.TITLE_BAR to color("#243039"),
            AppearanceElementTarget.TITLE_BUTTON to color("#2a3441"),
            AppearanceElementTarget.TITLE_BUTTON_BORDER to color("#3a4d5e"),
            AppearanceElementTarget.HAMBURGER_BACKGROUND to color("#2a3441"),
            AppearanceElementTarget.HAMBURGER_BORDER to color("#3a4d5e"),
            AppearanceElementTarget.HAMBURGER_BARS to color("#8ab4c4"),
            AppearanceElementTarget.THEME_TOGGLE_TRACK to color(mode.pick(light = "#e0e0e0", dark = "#2e3f4e")),
            AppearanceElementTarget.THEME_TOGGLE_SELECTED_TRACK to color("#4a7a8a"),
            AppearanceElementTarget.THEME_TOGGLE_KNOB to color("#ffffff"),
            AppearanceElementTarget.ACCENT_TEXT to color("#8ab4c4"),
            AppearanceElementTarget.ERROR_TEXT to color("#b5544d"),
            AppearanceElementTarget.STATUS_TEXT to color(mode.pick(light = "#4a7a8a", dark = "#8ab4c4")),
            AppearanceElementTarget.MENU_DISABLED_TEXT to color(mode.pick(light = "#b8b8b8", dark = "#a0b0bc")),
            AppearanceElementTarget.INTERACTIVE_HOVER_TEXT to color(mode.pick(light = "#8b0101", dark = "#c0c8d0")),
            AppearanceElementTarget.PRIMARY_BUTTON_FILL to color("#4a7a8a"),
            AppearanceElementTarget.SELECTED_ITEM_FILL to color(mode.pick(light = "#fafaf8", dark = "#222e3a")),
            AppearanceElementTarget.MENU_BACKGROUND to color(mode.pick(light = "#ffffff", dark = "#2a3441")),
            AppearanceElementTarget.MENU_HOVER to color(mode.pick(light = "#fafaf8", dark = "#1e2a35")),
            AppearanceElementTarget.STATUS_PILL to color(mode.pick(light = "#fafaf8", dark = "#1a2530")),
            AppearanceElementTarget.RULES_AND_SEPARATORS to color(mode.pick(light = "#e0e0e0", dark = "#2e3f4e")),
        )

    private fun <K, V> completeTargets(
        targets: List<K>,
        partial: Map<K, V>,
        fallback: (K) -> V,
    ): Map<K, V> =
        targets.associateWith { target ->
            partial[target] ?: fallback(target)
        }

    private fun color(value: String): AppearanceColor =
        AppearanceColor.require(value)

    private fun AppearanceMode.pick(light: String, dark: String): String =
        when (this) {
            AppearanceMode.LIGHT -> light
            AppearanceMode.DARK -> dark
        }

    private const val STOCK_CUSTOM_FALLBACK_FONT = "Inter"
}
