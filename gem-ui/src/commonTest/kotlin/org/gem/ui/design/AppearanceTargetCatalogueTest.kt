package org.gem.ui.design

import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppearanceTargetCatalogueTest {
    @Test
    fun textTargetsMatchCoreOrderPrototypeIdsAndLabelKeys() {
        val targets = AppearanceTargetCatalogue.textTargets

        assertEquals(AppearanceTextTarget.entries, targets.map { it.target })
        assertEquals(AppearanceTextTarget.entries.map { it.storageKey }, targets.map { it.prototypeId })
        assertEquals(
            listOf(
                GemTextKey.AppearanceTextTitleBar,
                GemTextKey.AppearanceTextTitleSubtitle,
                GemTextKey.AppearanceTextLogo,
                GemTextKey.AppearanceTextSectionHeadings,
                GemTextKey.AppearanceTextMainBody,
                GemTextKey.AppearanceTextFieldText,
                GemTextKey.AppearanceTextSmallLabels,
                GemTextKey.AppearanceTextButtonLabels,
                GemTextKey.AppearanceTextMenuLabels,
                GemTextKey.AppearanceTextSltClock,
                GemTextKey.AppearanceTextBackButton,
                GemTextKey.AppearanceTextThemeToggleLabels,
            ),
            targets.map { it.labelKey },
        )
        assertEquals(
            listOf(
                AppearanceTextColorSlot.BRAND_TITLE,
                AppearanceTextColorSlot.BRAND_SUBTITLE,
                AppearanceTextColorSlot.BRAND_LOGO,
                AppearanceTextColorSlot.SECTION,
                AppearanceTextColorSlot.BODY,
                AppearanceTextColorSlot.FIELD_TEXT,
                AppearanceTextColorSlot.SMALL_LABEL,
                AppearanceTextColorSlot.BUTTON_LABEL,
                AppearanceTextColorSlot.MENU_LABEL,
                AppearanceTextColorSlot.CLOCK,
                AppearanceTextColorSlot.BACK,
                AppearanceTextColorSlot.THEME_TOGGLE,
            ),
            targets.map { it.colorSlot },
        )
        assertEquals(listOf("sans-serif"), targets.map { it.defaultFontFamily.value }.distinct())
    }

    @Test
    fun elementTargetsMatchCoreOrderPrototypeIdsAndLabelKeys() {
        val targets = AppearanceTargetCatalogue.elementTargets

        assertEquals(AppearanceElementTarget.entries, targets.map { it.target })
        assertEquals(AppearanceElementTarget.entries.map { it.storageKey }, targets.map { it.prototypeId })
        assertEquals(
            listOf(
                GemTextKey.AppearanceElementPageBackground,
                GemTextKey.AppearanceElementCardBackground,
                GemTextKey.AppearanceElementPanelBackground,
                GemTextKey.AppearanceElementFieldBackground,
                GemTextKey.AppearanceElementFieldControlBorders,
                GemTextKey.AppearanceElementTitleBar,
                GemTextKey.AppearanceElementTitleButton,
                GemTextKey.AppearanceElementTitleButtonBorder,
                GemTextKey.AppearanceElementHamburgerBackground,
                GemTextKey.AppearanceElementHamburgerBorder,
                GemTextKey.AppearanceElementHamburgerBars,
                GemTextKey.AppearanceElementThemeToggleTrack,
                GemTextKey.AppearanceElementThemeToggleSelectedTrack,
                GemTextKey.AppearanceElementThemeToggleKnob,
                GemTextKey.AppearanceElementAccentText,
                GemTextKey.AppearanceElementErrorText,
                GemTextKey.AppearanceElementStatusText,
                GemTextKey.AppearanceElementMenuDisabledText,
                GemTextKey.AppearanceElementInteractiveHoverText,
                GemTextKey.AppearanceElementPrimaryButtonFill,
                GemTextKey.AppearanceElementSelectedItemFill,
                GemTextKey.AppearanceElementMenuBackground,
                GemTextKey.AppearanceElementMenuHover,
                GemTextKey.AppearanceElementStatusPill,
                GemTextKey.AppearanceElementRulesAndSeparators,
            ),
            targets.map { it.labelKey },
        )
        assertEquals(AppearanceElementColorSlot.entries, targets.map { it.colorSlot })
    }

    @Test
    fun targetPrototypeIdsAreUnique() {
        assertEquals(
            AppearanceTargetCatalogue.textTargets.size,
            AppearanceTargetCatalogue.textTargets.map { it.prototypeId }.toSet().size,
        )
        assertEquals(
            AppearanceTargetCatalogue.elementTargets.size,
            AppearanceTargetCatalogue.elementTargets.map { it.prototypeId }.toSet().size,
        )
    }

    @Test
    fun everyTextTargetHasMapperConsumerAndProofCoverage() {
        val rows = TEXT_TARGET_COVERAGE
        val byTarget = rows.associateBy { it.target }

        assertEquals(AppearanceTargetCatalogue.textTargets.map { it.target }.toSet(), byTarget.keys)
        assertEquals(rows.size, byTarget.size)

        AppearanceTargetCatalogue.textTargets.forEach { spec ->
            val row = byTarget.getValue(spec.target)
            assertEquals(spec.colorSlot, row.colorSlot, spec.target.name)
            assertEquals(spec.typeSlot, row.typeSlot, spec.target.name)
            row.assertComplete(spec.target.name)
        }
    }

    @Test
    fun everyElementTargetHasMapperConsumerAndProofCoverage() {
        val rows = ELEMENT_TARGET_COVERAGE
        val byTarget = rows.associateBy { it.target }

        assertEquals(AppearanceTargetCatalogue.elementTargets.map { it.target }.toSet(), byTarget.keys)
        assertEquals(rows.size, byTarget.size)

        AppearanceTargetCatalogue.elementTargets.forEach { spec ->
            val row = byTarget.getValue(spec.target)
            assertEquals(spec.colorSlot, row.colorSlot, spec.target.name)
            row.assertComplete(spec.target.name)
        }
    }

    private data class TextTargetCoverage(
        val target: AppearanceTextTarget,
        val colorSlot: AppearanceTextColorSlot,
        val typeSlot: AppearanceTypeSlot,
        val mapperOutput: String,
        val centralConsumer: String? = null,
        val nonRenderingGuard: String? = null,
        val proofOwner: String,
        val redPathAssertion: String,
    ) {
        fun assertComplete(context: String) {
            assertTrue(mapperOutput.isNotBlank(), context)
            assertTrue(proofOwner.isNotBlank(), context)
            assertTrue(redPathAssertion.isNotBlank(), context)
            assertTrue((centralConsumer != null) xor (nonRenderingGuard != null), context)
        }
    }

    private data class ElementTargetCoverage(
        val target: AppearanceElementTarget,
        val colorSlot: AppearanceElementColorSlot,
        val mapperOutput: String,
        val centralConsumer: String? = null,
        val nonRenderingGuard: String? = null,
        val proofOwner: String,
        val redPathAssertion: String,
    ) {
        fun assertComplete(context: String) {
            assertTrue(mapperOutput.isNotBlank(), context)
            assertTrue(proofOwner.isNotBlank(), context)
            assertTrue(redPathAssertion.isNotBlank(), context)
            assertTrue((centralConsumer != null) xor (nonRenderingGuard != null), context)
        }
    }

    private companion object {
        val TEXT_TARGET_COVERAGE: List<TextTargetCoverage> = listOf(
            TextTargetCoverage(
                target = AppearanceTextTarget.TITLE_BAR,
                colorSlot = AppearanceTextColorSlot.BRAND_TITLE,
                typeSlot = AppearanceTypeSlot.BRAND_TITLE,
                mapperOutput = "GemColors.brandInitialsInk; GemTypeScale.brandTitle",
                centralConsumer = "GemPlatformTopBarChrome title slot",
                proofOwner = "GemAppScaffoldTest; AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Settings and Accounts top-bar titles must not render brand subtitle or body styling.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.TITLE_SUBTITLE,
                colorSlot = AppearanceTextColorSlot.BRAND_SUBTITLE,
                typeSlot = AppearanceTypeSlot.BRAND_SUBTITLE,
                mapperOutput = "GemColors.brandWordmark; GemTypeScale.brandSubtitle",
                centralConsumer = "GemPlatformTopBarChrome optional subtitle slot",
                proofOwner = "GemAppScaffoldTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Login and Compose must keep the brand subtitle while section routes omit it.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.LOGO,
                colorSlot = AppearanceTextColorSlot.BRAND_LOGO,
                typeSlot = AppearanceTypeSlot.BRAND_TITLE,
                mapperOutput = "GemColors.brandMark and GemColors.brandAccent; GemTypeScale.brandTitle",
                centralConsumer = "GemBrandLogoIcon",
                proofOwner = "GemDesignTokenTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Logo colour must remain routed through the central brand icon owner.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.SECTION_HEADINGS,
                colorSlot = AppearanceTextColorSlot.SECTION,
                typeSlot = AppearanceTypeSlot.SECTION_TITLE,
                mapperOutput = "GemColors.ink; GemTypeScale.sectionTitle",
                centralConsumer = "GemModal and section-heading text owners",
                proofOwner = "AppearanceTargetCatalogueTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Section heading target must keep an atomic row and cannot be aggregate-proved by body text.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.MAIN_BODY,
                colorSlot = AppearanceTextColorSlot.BODY,
                typeSlot = AppearanceTypeSlot.BODY,
                mapperOutput = "GemColors.body; GemTypeScale.body",
                centralConsumer = "body text owners including modal and panel content",
                proofOwner = "AppearanceDesignTokenMapperTest",
                redPathAssertion = "Main body target must not be replaced by field-text or button-label tokens.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.FIELD_TEXT,
                colorSlot = AppearanceTextColorSlot.FIELD_TEXT,
                typeSlot = AppearanceTypeSlot.BODY,
                mapperOutput = "GemColors.ink; GemTypeScale.fieldText",
                centralConsumer = "GemFieldTokens and GemDropdownTokens selector text",
                proofOwner = "GemFieldsTest; GemDropdownsTest; AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Field text must not be rendered through button, menu, or small-label tokens.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.SMALL_LABELS,
                colorSlot = AppearanceTextColorSlot.SMALL_LABEL,
                typeSlot = AppearanceTypeSlot.SMALL_LABEL,
                mapperOutput = "GemColors.muted; GemTypeScale.smallLabel",
                centralConsumer = "GemFieldLabel and GemRgbSliderRow channel labels",
                proofOwner = "GemFieldsTest; GemRgbSliderRowTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Field labels and RGB labels must not read colors.secondary.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.BUTTON_LABELS,
                colorSlot = AppearanceTextColorSlot.BUTTON_LABEL,
                typeSlot = AppearanceTypeSlot.BUTTON,
                mapperOutput = "GemColors.buttonLabelInk and primaryInk; GemTypeScale.button",
                centralConsumer = "GemButtons and action-card button owners",
                proofOwner = "AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Button-label changes must not alter ThemeModeToggle labels.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.MENU_LABELS,
                colorSlot = AppearanceTextColorSlot.MENU_LABEL,
                typeSlot = AppearanceTypeSlot.MENU_ITEM,
                mapperOutput = "GemColors.topBarMenuInk; GemTypeScale.menuItem",
                centralConsumer = "GemMenuTextTokens plus Android/JVM overflow menu chrome",
                proofOwner = "GemDropdownsTest; GemPlatformOverflowMenuChromeTest; AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Menu rows must not read colors.secondary.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.SLT_CLOCK,
                colorSlot = AppearanceTextColorSlot.CLOCK,
                typeSlot = AppearanceTypeSlot.SMALL_LABEL,
                mapperOutput = "GemColors.topBarClockInk; GemTypeScale.smallLabel",
                centralConsumer = "GemPlatformTopBarChrome SLT clock",
                proofOwner = "GemAppScaffoldTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "SLT clock must not be routed through menu-label text by component code.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.BACK_BUTTON,
                colorSlot = AppearanceTextColorSlot.BACK,
                typeSlot = AppearanceTypeSlot.SMALL_LABEL,
                mapperOutput = "GemColors.navigationInk; GemTypeScale.backLabel",
                centralConsumer = "SectionBackNav",
                proofOwner = "SectionBackNavTest; AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Back label and arrow must not read smallLabel or colors.secondary directly.",
            ),
            TextTargetCoverage(
                target = AppearanceTextTarget.THEME_TOGGLE_LABELS,
                colorSlot = AppearanceTextColorSlot.THEME_TOGGLE,
                typeSlot = AppearanceTypeSlot.SMALL_LABEL,
                mapperOutput = "GemColors.themeToggleLabelInk; GemTypeScale.themeToggleLabel",
                centralConsumer = "ThemeModeToggle",
                proofOwner = "ThemeModeToggleTest; AppearanceControllerTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Theme toggle labels must not alias button labels.",
            ),
        )

        val ELEMENT_TARGET_COVERAGE: List<ElementTargetCoverage> = listOf(
            ElementTargetCoverage(
                target = AppearanceElementTarget.PAGE_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.PAGE,
                mapperOutput = "GemColors.page",
                centralConsumer = "GemAppScaffold page surface",
                proofOwner = "AppearanceControllerTest; GemDesignTokenTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Page background must not be a page-local colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.CARD_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.CARD,
                mapperOutput = "GemColors.surfaceStrong",
                centralConsumer = "card and expandable-panel owners",
                proofOwner = "GemDesignTokenTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Card background must not be hardcoded in panels.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.PANEL_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.PANEL,
                mapperOutput = "GemColors.surface",
                centralConsumer = "panel/content surface owners",
                proofOwner = "GemDesignTokenTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Panel background must not duplicate the card background route.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.FIELD_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.FIELD,
                mapperOutput = "GemColors.fieldSurface",
                centralConsumer = "GemFields, GemDropdowns, and compact field owners",
                proofOwner = "GemFieldsTest; GemDropdownsTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Field background must not be page-local.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.FIELD_CONTROL_BORDERS,
                colorSlot = AppearanceElementColorSlot.FIELD_BORDER,
                mapperOutput = "GemColors.fieldBorder, lineStrong, and toggleBorder",
                centralConsumer = "field, dropdown, toggle, and divider border owners",
                proofOwner = "GemFieldsTest; ThemeModeToggleTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Field/control borders must not use untracked local border colours.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.TITLE_BAR,
                colorSlot = AppearanceElementColorSlot.TITLE_BAR,
                mapperOutput = "GemColors.topBar",
                centralConsumer = "GemPlatformTopBarChrome",
                proofOwner = "GemAppScaffoldTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Title bar background must stay in the top-bar chrome owner.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.TITLE_BUTTON,
                colorSlot = AppearanceElementColorSlot.TITLE_BUTTON,
                mapperOutput = "GemColors.topBarButtonSurface",
                centralConsumer = "GemTopBarIconButton",
                proofOwner = "GemDesignTokenTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Title button fill must not be hand-rolled at top-bar callsites.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.TITLE_BUTTON_BORDER,
                colorSlot = AppearanceElementColorSlot.TITLE_BUTTON_BORDER,
                mapperOutput = "GemColors.topBarButtonBorder",
                centralConsumer = "GemTopBarIconButton",
                proofOwner = "GemDesignTokenTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Title button border must not bypass GemTopBarIconButton.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.HAMBURGER_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.HAMBURGER_BACKGROUND,
                mapperOutput = "GemColors.topBarButtonSurface",
                centralConsumer = "GemTopBarIconButton menu callsite",
                proofOwner = "GemAppScaffoldTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Hamburger background must not introduce a second menu-button owner.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.HAMBURGER_BORDER,
                colorSlot = AppearanceElementColorSlot.HAMBURGER_BORDER,
                mapperOutput = "GemColors.topBarButtonBorder",
                centralConsumer = "GemTopBarIconButton menu callsite",
                proofOwner = "GemAppScaffoldTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Hamburger border must not introduce page-local button styling.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.HAMBURGER_BARS,
                colorSlot = AppearanceElementColorSlot.HAMBURGER_BARS,
                mapperOutput = "GemColors.topBarButtonInk",
                centralConsumer = "GemMenuIcon",
                proofOwner = "GemAppScaffoldTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Hamburger bars must not use a local icon tint.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.THEME_TOGGLE_TRACK,
                colorSlot = AppearanceElementColorSlot.TOGGLE_TRACK,
                mapperOutput = "GemColors.toggleTrack",
                centralConsumer = "ThemeModeToggle unchecked track",
                proofOwner = "ThemeModeToggleTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Unchecked toggle track must not share selected-track colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.THEME_TOGGLE_SELECTED_TRACK,
                colorSlot = AppearanceElementColorSlot.TOGGLE_TRACK_SELECTED,
                mapperOutput = "GemColors.toggleTrackSelected",
                centralConsumer = "ThemeModeToggle checked track",
                proofOwner = "ThemeModeToggleTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Checked toggle track must not share unchecked-track colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.THEME_TOGGLE_KNOB,
                colorSlot = AppearanceElementColorSlot.TOGGLE_KNOB,
                mapperOutput = "GemColors.toggleKnob",
                centralConsumer = "ThemeModeToggle knob",
                proofOwner = "ThemeModeToggleTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Toggle knob must not use field background as a hidden fallback.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.ACCENT_TEXT,
                colorSlot = AppearanceElementColorSlot.ACCENT_TEXT,
                mapperOutput = "GemColors.secondary",
                centralConsumer = "central accent/icon owners",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Accent text must remain a selectable element target, not a hidden constant.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.ERROR_TEXT,
                colorSlot = AppearanceElementColorSlot.ERROR_TEXT,
                mapperOutput = "GemColors.danger",
                centralConsumer = "validation and error text owners",
                proofOwner = "GemFieldsTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Error text must not be generated from button or body colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.STATUS_TEXT,
                colorSlot = AppearanceElementColorSlot.STATUS_TEXT,
                mapperOutput = "GemColors.successInk",
                centralConsumer = "status strip and status text owners",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Status text must not use menu disabled or body colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.MENU_DISABLED_TEXT,
                colorSlot = AppearanceElementColorSlot.MENU_DISABLED_TEXT,
                mapperOutput = "GemColors.menuDisabledInk",
                centralConsumer = "GemMenuTextTokens disabled rows",
                proofOwner = "GemDropdownsTest; GemPlatformOverflowMenuChromeTest",
                redPathAssertion = "Disabled menu rows must not use enabled menu colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.INTERACTIVE_HOVER_TEXT,
                colorSlot = AppearanceElementColorSlot.INTERACTIVE_HOVER_TEXT,
                mapperOutput = "GemColors.interactiveHoverInk",
                centralConsumer = "SectionBackNav hover state",
                proofOwner = "SectionBackNavTest; AppearanceDesignTokenMapperTest",
                redPathAssertion = "Back hover must not use normal navigation colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.PRIMARY_BUTTON_FILL,
                colorSlot = AppearanceElementColorSlot.PRIMARY,
                mapperOutput = "GemColors.primary",
                centralConsumer = "GemButtons primary fill and range accent",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Primary fill must not be a button-label colour.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.SELECTED_ITEM_FILL,
                colorSlot = AppearanceElementColorSlot.SELECTED,
                mapperOutput = "GemColors.selectedBackground and menuActive",
                centralConsumer = "selected menu and segmented-control owners",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Selected item fill must not be derived from hover fill.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.MENU_BACKGROUND,
                colorSlot = AppearanceElementColorSlot.MENU,
                mapperOutput = "GemColors.menuSurface",
                centralConsumer = "GemDropdowns and platform overflow menus",
                proofOwner = "GemDropdownsTest; GemPlatformOverflowMenuChromeTest",
                redPathAssertion = "Menu background must not use page or card background by callsite.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.MENU_HOVER,
                colorSlot = AppearanceElementColorSlot.MENU_HOVER,
                mapperOutput = "GemColors.menuHover",
                centralConsumer = "GemDropdowns and platform context/menu hover owners",
                proofOwner = "GemDropdownsTest; GemPlatformOverflowMenuChromeTest",
                redPathAssertion = "Menu hover must not use selected item fill.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.STATUS_PILL,
                colorSlot = AppearanceElementColorSlot.STATUS,
                mapperOutput = "GemColors.statusBackground and successBackground",
                centralConsumer = "SessionStrip status pill",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Status pill fill must not be hardcoded in SessionStrip.",
            ),
            ElementTargetCoverage(
                target = AppearanceElementTarget.RULES_AND_SEPARATORS,
                colorSlot = AppearanceElementColorSlot.SEPARATOR,
                mapperOutput = "GemColors.line and shellBorder",
                centralConsumer = "SectionBackNav divider and scaffold borders",
                proofOwner = "AppearanceDesignTokenMapperTest; AppearancePaletteCoverageTest",
                redPathAssertion = "Rules and separators must not use page-local divider colours.",
            ),
        )
    }
}
