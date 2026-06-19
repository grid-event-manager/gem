package org.gem.core.appearance

object AppearanceProfileCatalogue {
    fun stockProfiles(): List<AppearanceProfile> =
        listOf(
            stockPrincessLight(),
            stockPrincessDark(),
            stockGothLight(),
            stockGothDark(),
            stockCyberLight(),
            stockCyberDark(),
        )

    fun systemProfile(
        mode: AppearanceMode,
        textFonts: Map<AppearanceTextTarget, AppearanceFontFamily>,
    ): AppearanceProfile {
        require(textFonts.keys == AppearanceTextTarget.entries.toSet()) {
            "System profile textFonts must cover every text target."
        }

        return AppearanceProfile(
            id = AppearanceProfileId(
                when (mode) {
                    AppearanceMode.LIGHT -> "system:light:gem-default"
                    AppearanceMode.DARK -> "system:dark:gem-default"
                },
            ),
            name = AppearanceProfileName("GEM Default"),
            mode = mode,
            source = AppearanceProfileSource.SYSTEM,
            textFonts = textFonts,
            textColors = signedOffTextColors(mode),
            elementColors = signedOffElementColors(mode),
        )
    }

    private fun stockFallbackTextFonts(): Map<AppearanceTextTarget, AppearanceFontFamily> =
        textTargetMap {
            AppearanceFontFamily("Inter")
        }

    private fun stockFallbackTextColors(mode: AppearanceMode): Map<AppearanceTextTarget, AppearanceColor> =
        signedOffTextColors(mode)

    private fun stockFallbackElementColors(mode: AppearanceMode): Map<AppearanceElementTarget, AppearanceColor> =
        signedOffElementColors(mode)

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
            AppearanceElementTarget.PRIMARY_BUTTON_FILL to color("#4a7a8a"),
            AppearanceElementTarget.SELECTED_ITEM_FILL to color(mode.pick(light = "#fafaf8", dark = "#222e3a")),
            AppearanceElementTarget.MENU_BACKGROUND to color(mode.pick(light = "#ffffff", dark = "#2a3441")),
            AppearanceElementTarget.MENU_HOVER to color(mode.pick(light = "#fafaf8", dark = "#1e2a35")),
            AppearanceElementTarget.STATUS_PILL to color(mode.pick(light = "#fafaf8", dark = "#1a2530")),
            AppearanceElementTarget.RULES_AND_SEPARATORS to color(mode.pick(light = "#e0e0e0", dark = "#2e3f4e")),
        )

    private fun stockPrincessLight(): AppearanceProfile =
        stockProfile(
            id = "stock-princess-light",
            name = "Princess",
            mode = AppearanceMode.LIGHT,
            fonts = mapOf(
                "title" to "Georgia",
                "subtitle" to "Trebuchet MS",
                "section" to "Trebuchet MS",
                "body" to "Inter",
                "field" to "Inter",
                "label" to "Inter",
                "button" to "Trebuchet MS",
                "menu" to "Inter",
                "clock" to "Inter",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "title" to "#ffffff",
                "subtitle" to "#fbe7f7",
                "section" to "#b85699",
                "body" to "#59485f",
                "field" to "#59485f",
                "label" to "#8a6b82",
                "button" to "#b85699",
                "menu" to "#b85699",
                "clock" to "#ffffff",
                "themeToggle" to "#b85699",
            ),
            areaColors = mapOf(
                "page" to "#f5edf8",
                "card" to "#ffffff",
                "panel" to "#fff7fd",
                "field" to "#fffafd",
                "fieldBorder" to "#d99ad0",
                "titleBar" to "#8bbfdc",
                "titleButton" to "#f1c7e6",
                "titleButtonBorder" to "#d99ad0",
                "brandDiamond" to "#ffffff",
                "hamburgerBackground" to "#f1c7e6",
                "hamburgerBorder" to "#d99ad0",
                "hamburgerBars" to "#ffffff",
                "toggleTrack" to "#f1c7e6",
                "toggleTrackSelected" to "#8bbfdc",
                "toggleKnob" to "#ffffff",
                "primary" to "#b85699",
                "selected" to "#fce6f4",
                "menu" to "#fffafd",
                "menuHover" to "#fce6f4",
                "status" to "#e7f4fb",
                "separator" to "#e6cde2",
            ),
        )

    private fun stockPrincessDark(): AppearanceProfile =
        stockProfile(
            id = "stock-princess-dark",
            name = "Princess",
            mode = AppearanceMode.DARK,
            fonts = mapOf(
                "title" to "Georgia",
                "subtitle" to "Trebuchet MS",
                "section" to "Trebuchet MS",
                "body" to "Inter",
                "field" to "Inter",
                "label" to "Inter",
                "button" to "Trebuchet MS",
                "menu" to "Inter",
                "clock" to "Inter",
                "back" to "Inter",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "brandLogo" to "#d9f0ff",
                "title" to "#fff3ff",
                "subtitle" to "#bfe9ff",
                "section" to "#ff9edb",
                "body" to "#e7edf8",
                "field" to "#fff3ff",
                "label" to "#acc3d8",
                "button" to "#ff9edb",
                "menu" to "#ff9edb",
                "clock" to "#ffffff",
                "back" to "#bfe9ff",
                "themeToggle" to "#bfe9ff",
            ),
            areaColors = mapOf(
                "page" to "#26374b",
                "card" to "#3f5872",
                "panel" to "#334a62",
                "field" to "#27394d",
                "fieldBorder" to "#88b8d4",
                "titleBar" to "#172a40",
                "titleButton" to "#263e57",
                "titleButtonBorder" to "#88b8d4",
                "brandDiamond" to "#d9f0ff",
                "hamburgerBackground" to "#263e57",
                "hamburgerBorder" to "#88b8d4",
                "hamburgerBars" to "#ff9edb",
                "toggleTrack" to "#2f4861",
                "toggleTrackSelected" to "#8bbfdc",
                "toggleKnob" to "#ffffff",
                "primary" to "#d76dac",
                "selected" to "#4d6882",
                "menu" to "#334a62",
                "menuHover" to "#4d6882",
                "status" to "#263e57",
                "separator" to "#5d7891",
            ),
        )

    private fun stockGothLight(): AppearanceProfile =
        stockProfile(
            id = "stock-goth-light",
            name = "Goth",
            mode = AppearanceMode.LIGHT,
            fonts = mapOf(
                "title" to "Georgia",
                "subtitle" to "Georgia",
                "section" to "Georgia",
                "body" to "Inter",
                "field" to "Inter",
                "label" to "Inter",
                "button" to "Inter",
                "menu" to "Inter",
                "clock" to "Inter",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "title" to "#f9f4f7",
                "subtitle" to "#d8b5c8",
                "section" to "#6c284f",
                "body" to "#362832",
                "field" to "#362832",
                "label" to "#745b70",
                "button" to "#6c284f",
                "menu" to "#6c284f",
                "clock" to "#ffffff",
                "themeToggle" to "#6c284f",
            ),
            areaColors = mapOf(
                "page" to "#ebe4e7",
                "card" to "#f8f2f5",
                "panel" to "#f0e8ec",
                "field" to "#fffafc",
                "fieldBorder" to "#a9829d",
                "titleBar" to "#231622",
                "titleButton" to "#342333",
                "titleButtonBorder" to "#6f4f6a",
                "brandDiamond" to "#d8b5c8",
                "hamburgerBackground" to "#342333",
                "hamburgerBorder" to "#6f4f6a",
                "hamburgerBars" to "#d8b5c8",
                "toggleTrack" to "#d7c8d3",
                "toggleTrackSelected" to "#6c284f",
                "toggleKnob" to "#ffffff",
                "primary" to "#6c284f",
                "selected" to "#eadbe5",
                "menu" to "#fffafc",
                "menuHover" to "#eadbe5",
                "status" to "#f1e4ec",
                "separator" to "#d7c8d3",
            ),
        )

    private fun stockGothDark(): AppearanceProfile =
        stockProfile(
            id = "stock-goth-dark",
            name = "Goth",
            mode = AppearanceMode.DARK,
            fonts = mapOf(
                "title" to "Georgia",
                "subtitle" to "Georgia",
                "section" to "Georgia",
                "body" to "Inter",
                "field" to "Inter",
                "label" to "Inter",
                "button" to "Inter",
                "menu" to "Inter",
                "clock" to "Inter",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "title" to "#f7edf8",
                "subtitle" to "#d4a0c2",
                "section" to "#d94f8c",
                "body" to "#d6c7d5",
                "field" to "#f1e5f0",
                "label" to "#a991a8",
                "button" to "#d94f8c",
                "menu" to "#d94f8c",
                "clock" to "#f7edf8",
                "themeToggle" to "#d94f8c",
            ),
            areaColors = mapOf(
                "page" to "#171219",
                "card" to "#241a27",
                "panel" to "#1d1620",
                "field" to "#161018",
                "fieldBorder" to "#60405e",
                "titleBar" to "#120e14",
                "titleButton" to "#201624",
                "titleButtonBorder" to "#60405e",
                "brandDiamond" to "#d94f8c",
                "hamburgerBackground" to "#201624",
                "hamburgerBorder" to "#60405e",
                "hamburgerBars" to "#d94f8c",
                "toggleTrack" to "#302236",
                "toggleTrackSelected" to "#7f2457",
                "toggleKnob" to "#f7edf8",
                "primary" to "#7f2457",
                "selected" to "#2f1f33",
                "menu" to "#1d1620",
                "menuHover" to "#2f1f33",
                "status" to "#211623",
                "separator" to "#3a293d",
            ),
        )

    private fun stockCyberLight(): AppearanceProfile =
        stockProfile(
            id = "stock-cyber-light",
            name = "Cyber",
            mode = AppearanceMode.LIGHT,
            fonts = mapOf(
                "title" to "Courier New",
                "subtitle" to "Inter",
                "section" to "Courier New",
                "body" to "Inter",
                "field" to "Courier New",
                "label" to "Inter",
                "button" to "Inter",
                "menu" to "Inter",
                "clock" to "Courier New",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "title" to "#f8feff",
                "subtitle" to "#83f7ff",
                "section" to "#006f86",
                "body" to "#20343c",
                "field" to "#20343c",
                "label" to "#53717a",
                "button" to "#006f86",
                "menu" to "#006f86",
                "clock" to "#ffffff",
                "themeToggle" to "#006f86",
            ),
            areaColors = mapOf(
                "page" to "#eef8fa",
                "card" to "#ffffff",
                "panel" to "#f6fcfd",
                "field" to "#ffffff",
                "fieldBorder" to "#30b8c9",
                "titleBar" to "#082430",
                "titleButton" to "#113846",
                "titleButtonBorder" to "#30b8c9",
                "brandDiamond" to "#83f7ff",
                "hamburgerBackground" to "#113846",
                "hamburgerBorder" to "#30b8c9",
                "hamburgerBars" to "#83f7ff",
                "toggleTrack" to "#d4edf2",
                "toggleTrackSelected" to "#00aac4",
                "toggleKnob" to "#ffffff",
                "primary" to "#00aac4",
                "selected" to "#dff6fa",
                "menu" to "#ffffff",
                "menuHover" to "#dff6fa",
                "status" to "#e7f8fb",
                "separator" to "#c2e8ef",
            ),
        )

    private fun stockCyberDark(): AppearanceProfile =
        stockProfile(
            id = "stock-cyber-dark",
            name = "Cyber",
            mode = AppearanceMode.DARK,
            fonts = mapOf(
                "title" to "Courier New",
                "subtitle" to "Inter",
                "section" to "Courier New",
                "body" to "Inter",
                "field" to "Courier New",
                "label" to "Inter",
                "button" to "Inter",
                "menu" to "Inter",
                "clock" to "Courier New",
                "themeToggle" to "Inter",
            ),
            textColors = mapOf(
                "title" to "#f8feff",
                "subtitle" to "#6dfaf0",
                "section" to "#00e5ff",
                "body" to "#bddbe0",
                "field" to "#e3fff9",
                "label" to "#83b5bf",
                "button" to "#00e5ff",
                "menu" to "#00e5ff",
                "clock" to "#f8feff",
                "themeToggle" to "#00e5ff",
            ),
            areaColors = mapOf(
                "page" to "#071720",
                "card" to "#102631",
                "panel" to "#0b202b",
                "field" to "#071720",
                "fieldBorder" to "#18d6c9",
                "titleBar" to "#061118",
                "titleButton" to "#0d2530",
                "titleButtonBorder" to "#18d6c9",
                "brandDiamond" to "#00e5ff",
                "hamburgerBackground" to "#0d2530",
                "hamburgerBorder" to "#18d6c9",
                "hamburgerBars" to "#00e5ff",
                "toggleTrack" to "#0d2530",
                "toggleTrackSelected" to "#18d6c9",
                "toggleKnob" to "#f8feff",
                "primary" to "#00a3b8",
                "selected" to "#123541",
                "menu" to "#0b202b",
                "menuHover" to "#123541",
                "status" to "#09202a",
                "separator" to "#16414a",
            ),
        )

    private fun stockProfile(
        id: String,
        name: String,
        mode: AppearanceMode,
        fonts: Map<String, String>,
        textColors: Map<String, String>,
        areaColors: Map<String, String>,
    ): AppearanceProfile {
        val fontOverrides = fonts.mapKeys { (key, _) -> textTargetFor(key) }
            .mapValues { (_, value) -> AppearanceFontFamily(value) }
        val textColorOverrides = textColors.mapKeys { (key, _) -> textTargetFor(key) }
            .mapValues { (_, value) -> color(value) }
        val logoColor = areaColors["brandDiamond"]?.let { color(it) }
        val elementOverrides = areaColors
            .filterKeys { it != "brandDiamond" }
            .mapKeys { (key, _) -> elementTargetFor(key) }
            .mapValues { (_, value) -> color(value) }

        return AppearanceProfile(
            id = AppearanceProfileId(id),
            name = AppearanceProfileName(name),
            mode = mode,
            source = AppearanceProfileSource.STOCK,
            textFonts = stockFallbackTextFonts() + fontOverrides,
            textColors = stockFallbackTextColors(mode) + textColorOverrides +
                listOfNotNull(logoColor?.let { AppearanceTextTarget.LOGO to it }).toMap(),
            elementColors = stockFallbackElementColors(mode) + elementOverrides,
        )
    }

    private fun textTargetFor(storageKey: String): AppearanceTextTarget =
        AppearanceTextTarget.entries.first { it.storageKey == storageKey }

    private fun elementTargetFor(storageKey: String): AppearanceElementTarget =
        AppearanceElementTarget.entries.first { it.storageKey == storageKey }

    private fun color(value: String): AppearanceColor =
        AppearanceColor.require(value)

    private fun AppearanceMode.pick(light: String, dark: String): String =
        when (this) {
            AppearanceMode.LIGHT -> light
            AppearanceMode.DARK -> dark
        }

    private fun <T> textTargetMap(value: (AppearanceTextTarget) -> T): Map<AppearanceTextTarget, T> =
        AppearanceTextTarget.entries.associateWith(value)
}
