package org.gem.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals

class GemTextCatalogueTest {
    @Test
    fun appearanceTextTargetCopyMatchesPrototypeContract() {
        val catalogue = EnglishGemTextCatalogue

        val copy = listOf(
            GemTextKey.AppearanceTextTitleBar to "Title bar",
            GemTextKey.AppearanceTextTitleSubtitle to "Title subtitle",
            GemTextKey.AppearanceTextLogo to "Logo",
            GemTextKey.AppearanceTextSectionHeadings to "Section headings",
            GemTextKey.AppearanceTextMainBody to "Main body",
            GemTextKey.AppearanceTextFieldText to "Field text",
            GemTextKey.AppearanceTextSmallLabels to "Small labels",
            GemTextKey.AppearanceTextButtonLabels to "Button labels",
            GemTextKey.AppearanceTextMenuLabels to "Menu labels",
            GemTextKey.AppearanceTextSltClock to "SLT clock",
            GemTextKey.AppearanceTextBackButton to "Back button",
            GemTextKey.AppearanceTextThemeToggleLabels to "Theme toggle labels",
        )

        copy.forEach { (key, expected) ->
            assertEquals(expected, catalogue.text(key))
        }
    }

    @Test
    fun appearanceElementTargetCopyMatchesPrototypeContract() {
        val catalogue = EnglishGemTextCatalogue

        val copy = listOf(
            GemTextKey.AppearanceElementPageBackground to "Page background",
            GemTextKey.AppearanceElementCardBackground to "Card background",
            GemTextKey.AppearanceElementPanelBackground to "Panel background",
            GemTextKey.AppearanceElementFieldBackground to "Field background",
            GemTextKey.AppearanceElementFieldControlBorders to "Field/control borders",
            GemTextKey.AppearanceElementTitleBar to "Title bar",
            GemTextKey.AppearanceElementTitleButton to "Title button",
            GemTextKey.AppearanceElementTitleButtonBorder to "Title button border",
            GemTextKey.AppearanceElementHamburgerBackground to "Hamburger background",
            GemTextKey.AppearanceElementHamburgerBorder to "Hamburger border",
            GemTextKey.AppearanceElementHamburgerBars to "Hamburger bars",
                GemTextKey.AppearanceElementThemeToggleTrack to "Theme toggle track",
                GemTextKey.AppearanceElementThemeToggleSelectedTrack to "Theme toggle selected track",
                GemTextKey.AppearanceElementThemeToggleKnob to "Theme toggle knob",
                GemTextKey.AppearanceElementAccentText to "Accent text",
                GemTextKey.AppearanceElementErrorText to "Error text",
                GemTextKey.AppearanceElementStatusText to "Status text",
                GemTextKey.AppearanceElementMenuDisabledText to "Menu disabled text",
                GemTextKey.AppearanceElementInteractiveHoverText to "Interactive hover text",
                GemTextKey.AppearanceElementPrimaryButtonFill to "Primary button fill",
            GemTextKey.AppearanceElementSelectedItemFill to "Selected item fill",
            GemTextKey.AppearanceElementMenuBackground to "Menu background",
            GemTextKey.AppearanceElementMenuHover to "Menu hover",
            GemTextKey.AppearanceElementStatusPill to "Status pill",
            GemTextKey.AppearanceElementRulesAndSeparators to "Rules and separators",
        )

        copy.forEach { (key, expected) ->
            assertEquals(expected, catalogue.text(key))
        }
    }
}
