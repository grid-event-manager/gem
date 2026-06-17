package org.gem.ui.design

import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.text.GemTextKey

data class AppearanceTextTargetSpec(
    val target: AppearanceTextTarget,
    val labelKey: GemTextKey,
    val prototypeId: String,
    val defaultFontFamily: AppearanceFontFamily,
    val colorSlot: AppearanceTextColorSlot,
    val typeSlot: AppearanceTypeSlot,
)

data class AppearanceElementTargetSpec(
    val target: AppearanceElementTarget,
    val labelKey: GemTextKey,
    val prototypeId: String,
    val colorSlot: AppearanceElementColorSlot,
)

enum class AppearanceTextColorSlot {
    BRAND_TITLE,
    BRAND_SUBTITLE,
    BRAND_LOGO,
    SECTION,
    BODY,
    FIELD_TEXT,
    SMALL_LABEL,
    BUTTON_LABEL,
    MENU_LABEL,
    CLOCK,
    BACK,
    THEME_TOGGLE,
}

enum class AppearanceElementColorSlot {
    PAGE,
    CARD,
    PANEL,
    FIELD,
    FIELD_BORDER,
    TITLE_BAR,
    TITLE_BUTTON,
    TITLE_BUTTON_BORDER,
    HAMBURGER_BACKGROUND,
    HAMBURGER_BORDER,
    HAMBURGER_BARS,
    TOGGLE_TRACK,
    TOGGLE_TRACK_SELECTED,
    TOGGLE_KNOB,
    PRIMARY,
    SELECTED,
    MENU,
    MENU_HOVER,
    STATUS,
    SEPARATOR,
}

enum class AppearanceTypeSlot {
    BRAND_TITLE,
    BRAND_SUBTITLE,
    SECTION_TITLE,
    BODY,
    SMALL_LABEL,
    BUTTON,
    MENU_ITEM,
}

object AppearanceTargetCatalogue {
    private val defaultFontFamily = AppearanceFontFamily("Inter")

    val textTargets: List<AppearanceTextTargetSpec> = listOf(
        text(
            target = AppearanceTextTarget.TITLE_BAR,
            labelKey = GemTextKey.AppearanceTextTitleBar,
            colorSlot = AppearanceTextColorSlot.BRAND_TITLE,
            typeSlot = AppearanceTypeSlot.BRAND_TITLE,
        ),
        text(
            target = AppearanceTextTarget.TITLE_SUBTITLE,
            labelKey = GemTextKey.AppearanceTextTitleSubtitle,
            colorSlot = AppearanceTextColorSlot.BRAND_SUBTITLE,
            typeSlot = AppearanceTypeSlot.BRAND_SUBTITLE,
        ),
        text(
            target = AppearanceTextTarget.LOGO,
            labelKey = GemTextKey.AppearanceTextLogo,
            colorSlot = AppearanceTextColorSlot.BRAND_LOGO,
            typeSlot = AppearanceTypeSlot.BRAND_TITLE,
        ),
        text(
            target = AppearanceTextTarget.SECTION_HEADINGS,
            labelKey = GemTextKey.AppearanceTextSectionHeadings,
            colorSlot = AppearanceTextColorSlot.SECTION,
            typeSlot = AppearanceTypeSlot.SECTION_TITLE,
        ),
        text(
            target = AppearanceTextTarget.MAIN_BODY,
            labelKey = GemTextKey.AppearanceTextMainBody,
            colorSlot = AppearanceTextColorSlot.BODY,
            typeSlot = AppearanceTypeSlot.BODY,
        ),
        text(
            target = AppearanceTextTarget.FIELD_TEXT,
            labelKey = GemTextKey.AppearanceTextFieldText,
            colorSlot = AppearanceTextColorSlot.FIELD_TEXT,
            typeSlot = AppearanceTypeSlot.BODY,
        ),
        text(
            target = AppearanceTextTarget.SMALL_LABELS,
            labelKey = GemTextKey.AppearanceTextSmallLabels,
            colorSlot = AppearanceTextColorSlot.SMALL_LABEL,
            typeSlot = AppearanceTypeSlot.SMALL_LABEL,
        ),
        text(
            target = AppearanceTextTarget.BUTTON_LABELS,
            labelKey = GemTextKey.AppearanceTextButtonLabels,
            colorSlot = AppearanceTextColorSlot.BUTTON_LABEL,
            typeSlot = AppearanceTypeSlot.BUTTON,
        ),
        text(
            target = AppearanceTextTarget.MENU_LABELS,
            labelKey = GemTextKey.AppearanceTextMenuLabels,
            colorSlot = AppearanceTextColorSlot.MENU_LABEL,
            typeSlot = AppearanceTypeSlot.MENU_ITEM,
        ),
        text(
            target = AppearanceTextTarget.SLT_CLOCK,
            labelKey = GemTextKey.AppearanceTextSltClock,
            colorSlot = AppearanceTextColorSlot.CLOCK,
            typeSlot = AppearanceTypeSlot.SMALL_LABEL,
        ),
        text(
            target = AppearanceTextTarget.BACK_BUTTON,
            labelKey = GemTextKey.AppearanceTextBackButton,
            colorSlot = AppearanceTextColorSlot.BACK,
            typeSlot = AppearanceTypeSlot.SMALL_LABEL,
        ),
        text(
            target = AppearanceTextTarget.THEME_TOGGLE_LABELS,
            labelKey = GemTextKey.AppearanceTextThemeToggleLabels,
            colorSlot = AppearanceTextColorSlot.THEME_TOGGLE,
            typeSlot = AppearanceTypeSlot.SMALL_LABEL,
        ),
    )

    val elementTargets: List<AppearanceElementTargetSpec> = listOf(
        element(
            target = AppearanceElementTarget.PAGE_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementPageBackground,
            colorSlot = AppearanceElementColorSlot.PAGE,
        ),
        element(
            target = AppearanceElementTarget.CARD_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementCardBackground,
            colorSlot = AppearanceElementColorSlot.CARD,
        ),
        element(
            target = AppearanceElementTarget.PANEL_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementPanelBackground,
            colorSlot = AppearanceElementColorSlot.PANEL,
        ),
        element(
            target = AppearanceElementTarget.FIELD_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementFieldBackground,
            colorSlot = AppearanceElementColorSlot.FIELD,
        ),
        element(
            target = AppearanceElementTarget.FIELD_CONTROL_BORDERS,
            labelKey = GemTextKey.AppearanceElementFieldControlBorders,
            colorSlot = AppearanceElementColorSlot.FIELD_BORDER,
        ),
        element(
            target = AppearanceElementTarget.TITLE_BAR,
            labelKey = GemTextKey.AppearanceElementTitleBar,
            colorSlot = AppearanceElementColorSlot.TITLE_BAR,
        ),
        element(
            target = AppearanceElementTarget.TITLE_BUTTON,
            labelKey = GemTextKey.AppearanceElementTitleButton,
            colorSlot = AppearanceElementColorSlot.TITLE_BUTTON,
        ),
        element(
            target = AppearanceElementTarget.TITLE_BUTTON_BORDER,
            labelKey = GemTextKey.AppearanceElementTitleButtonBorder,
            colorSlot = AppearanceElementColorSlot.TITLE_BUTTON_BORDER,
        ),
        element(
            target = AppearanceElementTarget.HAMBURGER_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementHamburgerBackground,
            colorSlot = AppearanceElementColorSlot.HAMBURGER_BACKGROUND,
        ),
        element(
            target = AppearanceElementTarget.HAMBURGER_BORDER,
            labelKey = GemTextKey.AppearanceElementHamburgerBorder,
            colorSlot = AppearanceElementColorSlot.HAMBURGER_BORDER,
        ),
        element(
            target = AppearanceElementTarget.HAMBURGER_BARS,
            labelKey = GemTextKey.AppearanceElementHamburgerBars,
            colorSlot = AppearanceElementColorSlot.HAMBURGER_BARS,
        ),
        element(
            target = AppearanceElementTarget.THEME_TOGGLE_TRACK,
            labelKey = GemTextKey.AppearanceElementThemeToggleTrack,
            colorSlot = AppearanceElementColorSlot.TOGGLE_TRACK,
        ),
        element(
            target = AppearanceElementTarget.THEME_TOGGLE_SELECTED_TRACK,
            labelKey = GemTextKey.AppearanceElementThemeToggleSelectedTrack,
            colorSlot = AppearanceElementColorSlot.TOGGLE_TRACK_SELECTED,
        ),
        element(
            target = AppearanceElementTarget.THEME_TOGGLE_KNOB,
            labelKey = GemTextKey.AppearanceElementThemeToggleKnob,
            colorSlot = AppearanceElementColorSlot.TOGGLE_KNOB,
        ),
        element(
            target = AppearanceElementTarget.PRIMARY_BUTTON_FILL,
            labelKey = GemTextKey.AppearanceElementPrimaryButtonFill,
            colorSlot = AppearanceElementColorSlot.PRIMARY,
        ),
        element(
            target = AppearanceElementTarget.SELECTED_ITEM_FILL,
            labelKey = GemTextKey.AppearanceElementSelectedItemFill,
            colorSlot = AppearanceElementColorSlot.SELECTED,
        ),
        element(
            target = AppearanceElementTarget.MENU_BACKGROUND,
            labelKey = GemTextKey.AppearanceElementMenuBackground,
            colorSlot = AppearanceElementColorSlot.MENU,
        ),
        element(
            target = AppearanceElementTarget.MENU_HOVER,
            labelKey = GemTextKey.AppearanceElementMenuHover,
            colorSlot = AppearanceElementColorSlot.MENU_HOVER,
        ),
        element(
            target = AppearanceElementTarget.STATUS_PILL,
            labelKey = GemTextKey.AppearanceElementStatusPill,
            colorSlot = AppearanceElementColorSlot.STATUS,
        ),
        element(
            target = AppearanceElementTarget.RULES_AND_SEPARATORS,
            labelKey = GemTextKey.AppearanceElementRulesAndSeparators,
            colorSlot = AppearanceElementColorSlot.SEPARATOR,
        ),
    )

    private fun text(
        target: AppearanceTextTarget,
        labelKey: GemTextKey,
        colorSlot: AppearanceTextColorSlot,
        typeSlot: AppearanceTypeSlot,
    ): AppearanceTextTargetSpec =
        AppearanceTextTargetSpec(
            target = target,
            labelKey = labelKey,
            prototypeId = target.storageKey,
            defaultFontFamily = defaultFontFamily,
            colorSlot = colorSlot,
            typeSlot = typeSlot,
        )

    private fun element(
        target: AppearanceElementTarget,
        labelKey: GemTextKey,
        colorSlot: AppearanceElementColorSlot,
    ): AppearanceElementTargetSpec =
        AppearanceElementTargetSpec(
            target = target,
            labelKey = labelKey,
            prototypeId = target.storageKey,
            colorSlot = colorSlot,
        )
}
