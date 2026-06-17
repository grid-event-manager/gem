package org.gem.core.appearance

enum class AppearanceTextTarget(
    val storageKey: String,
) {
    TITLE_BAR("title"),
    TITLE_SUBTITLE("subtitle"),
    LOGO("brandLogo"),
    SECTION_HEADINGS("section"),
    MAIN_BODY("body"),
    FIELD_TEXT("field"),
    SMALL_LABELS("label"),
    BUTTON_LABELS("button"),
    MENU_LABELS("menu"),
    SLT_CLOCK("clock"),
    BACK_BUTTON("back"),
    THEME_TOGGLE_LABELS("themeToggle"),
}
