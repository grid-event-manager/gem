package org.gem.ui.design

import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
