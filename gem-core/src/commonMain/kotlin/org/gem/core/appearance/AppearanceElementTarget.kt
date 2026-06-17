package org.gem.core.appearance

enum class AppearanceElementTarget(
    val storageKey: String,
) {
    PAGE_BACKGROUND("page"),
    CARD_BACKGROUND("card"),
    PANEL_BACKGROUND("panel"),
    FIELD_BACKGROUND("field"),
    FIELD_CONTROL_BORDERS("fieldBorder"),
    TITLE_BAR("titleBar"),
    TITLE_BUTTON("titleButton"),
    TITLE_BUTTON_BORDER("titleButtonBorder"),
    HAMBURGER_BACKGROUND("hamburgerBackground"),
    HAMBURGER_BORDER("hamburgerBorder"),
    HAMBURGER_BARS("hamburgerBars"),
    THEME_TOGGLE_TRACK("toggleTrack"),
    THEME_TOGGLE_SELECTED_TRACK("toggleTrackSelected"),
    THEME_TOGGLE_KNOB("toggleKnob"),
    PRIMARY_BUTTON_FILL("primary"),
    SELECTED_ITEM_FILL("selected"),
    MENU_BACKGROUND("menu"),
    MENU_HOVER("menuHover"),
    STATUS_PILL("status"),
    RULES_AND_SEPARATORS("separator"),
}
