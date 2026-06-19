package org.gem.ui.design

import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceTextTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppearancePaletteCoverageTest {
    @Test
    fun coverageTableMatchesGemColorsConstructorSurface() {
        assertEquals(GEM_COLORS_CONSTRUCTOR_PROPERTIES, PALETTE_COVERAGE.map { it.propertyName }.toSet())
        assertEquals(PALETTE_COVERAGE.size, PALETTE_COVERAGE.map { it.propertyName }.toSet().size)
    }

    @Test
    fun eachCoverageEntryHasOneOwnerCategory() {
        PALETTE_COVERAGE.forEach { entry ->
            when (entry.owner) {
                is PaletteOwner.TextTarget -> assertTrue(entry.owner.target in AppearanceTextTarget.entries)
                is PaletteOwner.ElementTarget -> assertTrue(entry.owner.target in AppearanceElementTarget.entries)
                is PaletteOwner.MapperDerived -> assertTrue(entry.owner.reason.isNotBlank())
                is PaletteOwner.NonVisibleCompileDefault -> assertTrue(entry.owner.reason.isNotBlank())
            }
        }
    }

    @Test
    fun nonVisibleCompileDefaultsAreNotSignedOffVisibleHousePaletteProperties() {
        PALETTE_COVERAGE
            .filter { it.owner is PaletteOwner.NonVisibleCompileDefault }
            .forEach { entry ->
                assertFalse(entry.propertyName in SIGNED_OFF_VISIBLE_HOUSE_PALETTE_PROPERTIES, entry.propertyName)
            }
    }

    @Test
    fun nonVisibleCompileDefaultsHaveNoProductionUiSourceRead() {
        val nonVisibleProperties = PALETTE_COVERAGE
            .filter { it.owner is PaletteOwner.NonVisibleCompileDefault }
            .map { it.propertyName }
            .toSet()

        assertTrue(nonVisibleProperties.isNotEmpty())
        assertEquals(emptySet(), nonVisibleProperties intersect PRODUCTION_UI_SOURCE_READ_PROPERTIES)
    }

    @Test
    fun signedOffVisibleHousePalettePropertiesAreNotMarkedNonVisible() {
        SIGNED_OFF_VISIBLE_HOUSE_PALETTE_PROPERTIES.forEach { propertyName ->
            val entry = PALETTE_COVERAGE.single { it.propertyName == propertyName }
            assertFalse(entry.owner is PaletteOwner.NonVisibleCompileDefault, propertyName)
        }
    }

    @Test
    fun rfcNamedNonVisibleCompileDefaultsStayDocumented() {
        val nonVisible = PALETTE_COVERAGE
            .filter { it.owner is PaletteOwner.NonVisibleCompileDefault }
            .associate { it.propertyName to assertIs<PaletteOwner.NonVisibleCompileDefault>(it.owner).reason }

        assertEquals(
            mapOf(
                "dangerFill" to "No production UI source reads dangerFill; danger UI uses danger.",
                "brandMarkInk" to "No production UI source reads brandMarkInk; logo drawing uses brandMark and brandAccent.",
                "focusRing" to "No production UI source reads focusRing; focus styling uses component-owned border tokens.",
            ),
            nonVisible,
        )
    }

    private data class PaletteCoverage(
        val propertyName: String,
        val owner: PaletteOwner,
    )

    private sealed interface PaletteOwner {
        data class TextTarget(val target: AppearanceTextTarget) : PaletteOwner
        data class ElementTarget(val target: AppearanceElementTarget) : PaletteOwner
        data class MapperDerived(val reason: String) : PaletteOwner
        data class NonVisibleCompileDefault(val reason: String) : PaletteOwner
    }

    private companion object {
        val GEM_COLORS_CONSTRUCTOR_PROPERTIES: Set<String> = setOf(
            "page",
            "surface",
            "surfaceStrong",
            "ink",
            "body",
            "muted",
            "line",
            "lineStrong",
            "primary",
            "primaryInk",
            "secondary",
            "danger",
            "dangerFill",
            "successBackground",
            "successInk",
            "selectedBackground",
            "topBar",
            "brandMark",
            "shellBorder",
            "disabledBackground",
            "disabledInk",
            "selectedInk",
            "topBarInk",
            "topBarMenuInk",
            "topBarClockInk",
            "themeToggleLabelInk",
            "topBarButtonSurface",
            "topBarButtonBorder",
            "topBarButtonInk",
            "brandMarkInk",
            "statusBackground",
            "focusRing",
            "menuSurface",
            "menuHover",
            "menuActive",
            "menuDisabledInk",
            "fieldSurface",
            "fieldBorder",
            "brandWordmark",
            "brandInitialsInk",
            "brandAccent",
            "buttonLabelInk",
            "toggleTrack",
            "toggleTrackSelected",
            "toggleKnob",
            "toggleBorder",
            "navigationInk",
            "interactiveHoverInk",
        )

        val SIGNED_OFF_VISIBLE_HOUSE_PALETTE_PROPERTIES: Set<String> =
            GEM_COLORS_CONSTRUCTOR_PROPERTIES - setOf("dangerFill", "brandMarkInk", "focusRing")

        val PRODUCTION_UI_SOURCE_READ_PROPERTIES: Set<String> = SIGNED_OFF_VISIBLE_HOUSE_PALETTE_PROPERTIES

        val PALETTE_COVERAGE: List<PaletteCoverage> = listOf(
            PaletteCoverage("page", PaletteOwner.ElementTarget(AppearanceElementTarget.PAGE_BACKGROUND)),
            PaletteCoverage("surface", PaletteOwner.ElementTarget(AppearanceElementTarget.PANEL_BACKGROUND)),
            PaletteCoverage("surfaceStrong", PaletteOwner.ElementTarget(AppearanceElementTarget.CARD_BACKGROUND)),
            PaletteCoverage("ink", PaletteOwner.TextTarget(AppearanceTextTarget.FIELD_TEXT)),
            PaletteCoverage("body", PaletteOwner.TextTarget(AppearanceTextTarget.MAIN_BODY)),
            PaletteCoverage("muted", PaletteOwner.TextTarget(AppearanceTextTarget.SMALL_LABELS)),
            PaletteCoverage("line", PaletteOwner.ElementTarget(AppearanceElementTarget.RULES_AND_SEPARATORS)),
            PaletteCoverage("lineStrong", PaletteOwner.ElementTarget(AppearanceElementTarget.FIELD_CONTROL_BORDERS)),
            PaletteCoverage("primary", PaletteOwner.ElementTarget(AppearanceElementTarget.PRIMARY_BUTTON_FILL)),
            PaletteCoverage("primaryInk", PaletteOwner.TextTarget(AppearanceTextTarget.BUTTON_LABELS)),
            PaletteCoverage("secondary", PaletteOwner.ElementTarget(AppearanceElementTarget.ACCENT_TEXT)),
            PaletteCoverage("danger", PaletteOwner.ElementTarget(AppearanceElementTarget.ERROR_TEXT)),
            PaletteCoverage(
                "dangerFill",
                PaletteOwner.NonVisibleCompileDefault("No production UI source reads dangerFill; danger UI uses danger."),
            ),
            PaletteCoverage("successBackground", PaletteOwner.ElementTarget(AppearanceElementTarget.STATUS_PILL)),
            PaletteCoverage("successInk", PaletteOwner.ElementTarget(AppearanceElementTarget.STATUS_TEXT)),
            PaletteCoverage("selectedBackground", PaletteOwner.ElementTarget(AppearanceElementTarget.SELECTED_ITEM_FILL)),
            PaletteCoverage("topBar", PaletteOwner.ElementTarget(AppearanceElementTarget.TITLE_BAR)),
            PaletteCoverage("brandMark", PaletteOwner.TextTarget(AppearanceTextTarget.LOGO)),
            PaletteCoverage("shellBorder", PaletteOwner.ElementTarget(AppearanceElementTarget.RULES_AND_SEPARATORS)),
            PaletteCoverage("disabledBackground", PaletteOwner.MapperDerived("disabledBackground = line")),
            PaletteCoverage("disabledInk", PaletteOwner.MapperDerived("disabledInk = muted")),
            PaletteCoverage(
                "selectedInk",
                PaletteOwner.MapperDerived("selectedInk = readableAccentOn(selectedBackground, secondary, primary)"),
            ),
            PaletteCoverage("topBarInk", PaletteOwner.MapperDerived("topBarInk = secondary")),
            PaletteCoverage("topBarMenuInk", PaletteOwner.TextTarget(AppearanceTextTarget.MENU_LABELS)),
            PaletteCoverage("topBarClockInk", PaletteOwner.TextTarget(AppearanceTextTarget.SLT_CLOCK)),
            PaletteCoverage("themeToggleLabelInk", PaletteOwner.TextTarget(AppearanceTextTarget.THEME_TOGGLE_LABELS)),
            PaletteCoverage("topBarButtonSurface", PaletteOwner.ElementTarget(AppearanceElementTarget.HAMBURGER_BACKGROUND)),
            PaletteCoverage("topBarButtonBorder", PaletteOwner.ElementTarget(AppearanceElementTarget.HAMBURGER_BORDER)),
            PaletteCoverage("topBarButtonInk", PaletteOwner.ElementTarget(AppearanceElementTarget.HAMBURGER_BARS)),
            PaletteCoverage(
                "brandMarkInk",
                PaletteOwner.NonVisibleCompileDefault(
                    "No production UI source reads brandMarkInk; logo drawing uses brandMark and brandAccent.",
                ),
            ),
            PaletteCoverage("statusBackground", PaletteOwner.ElementTarget(AppearanceElementTarget.STATUS_PILL)),
            PaletteCoverage(
                "focusRing",
                PaletteOwner.NonVisibleCompileDefault(
                    "No production UI source reads focusRing; focus styling uses component-owned border tokens.",
                ),
            ),
            PaletteCoverage("menuSurface", PaletteOwner.ElementTarget(AppearanceElementTarget.MENU_BACKGROUND)),
            PaletteCoverage("menuHover", PaletteOwner.ElementTarget(AppearanceElementTarget.MENU_HOVER)),
            PaletteCoverage("menuActive", PaletteOwner.ElementTarget(AppearanceElementTarget.SELECTED_ITEM_FILL)),
            PaletteCoverage("menuDisabledInk", PaletteOwner.ElementTarget(AppearanceElementTarget.MENU_DISABLED_TEXT)),
            PaletteCoverage("fieldSurface", PaletteOwner.ElementTarget(AppearanceElementTarget.FIELD_BACKGROUND)),
            PaletteCoverage("fieldBorder", PaletteOwner.ElementTarget(AppearanceElementTarget.FIELD_CONTROL_BORDERS)),
            PaletteCoverage("brandWordmark", PaletteOwner.TextTarget(AppearanceTextTarget.TITLE_SUBTITLE)),
            PaletteCoverage("brandInitialsInk", PaletteOwner.TextTarget(AppearanceTextTarget.TITLE_BAR)),
            PaletteCoverage("brandAccent", PaletteOwner.TextTarget(AppearanceTextTarget.LOGO)),
            PaletteCoverage("buttonLabelInk", PaletteOwner.TextTarget(AppearanceTextTarget.BUTTON_LABELS)),
            PaletteCoverage("toggleTrack", PaletteOwner.ElementTarget(AppearanceElementTarget.THEME_TOGGLE_TRACK)),
            PaletteCoverage(
                "toggleTrackSelected",
                PaletteOwner.ElementTarget(AppearanceElementTarget.THEME_TOGGLE_SELECTED_TRACK),
            ),
            PaletteCoverage("toggleKnob", PaletteOwner.ElementTarget(AppearanceElementTarget.THEME_TOGGLE_KNOB)),
            PaletteCoverage("toggleBorder", PaletteOwner.ElementTarget(AppearanceElementTarget.FIELD_CONTROL_BORDERS)),
            PaletteCoverage("navigationInk", PaletteOwner.TextTarget(AppearanceTextTarget.BACK_BUTTON)),
            PaletteCoverage("interactiveHoverInk", PaletteOwner.ElementTarget(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT)),
        )
    }
}
