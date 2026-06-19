package org.gem.ui.components

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.testing.controllerBackedAppearanceState
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppearanceCustomisePanelTest {
    private val text = EnglishGemTextCatalogue

    @Test
    fun layoutOrderMatchesPrototypeWithFontsHiddenAndVisible() {
        val hidden = controllerBackedAppearanceState(osDark = false)
        val visible = hidden.copy(fontsVisible = true)

        assertEquals(
            listOf("color-picker", "target-row", "actions"),
            AppearanceCustomisePanelInteraction.layoutOrder(hidden),
        )
        assertEquals(
            listOf("color-picker", "target-row", "fonts", "actions"),
            AppearanceCustomisePanelInteraction.layoutOrder(visible),
        )
    }

    @Test
    fun textAndElementDropdownsStartWithDisabledPlaceholderOptions() {
        val textOptions = AppearanceCustomisePanelInteraction.textTargetOptions(text)
        val elementOptions = AppearanceCustomisePanelInteraction.elementTargetOptions(text)

        assertEquals(text.text(GemTextKey.Text), textOptions.first().label)
        assertEquals(null, textOptions.first().value)
        assertFalse(textOptions.first().enabled)
        assertEquals(text.text(GemTextKey.Element), elementOptions.first().label)
        assertEquals(null, elementOptions.first().value)
        assertFalse(elementOptions.first().enabled)
    }

    @Test
    fun fontsDropdownStartsWithDisabledPlaceholderAndKeepsStateOrder() {
        val state = controllerBackedAppearanceState(
            osDark = false,
            availableFontFamilies = listOf(AppearanceFontFamily("Inter"), AppearanceFontFamily("Atkinson")),
        )
        val options = AppearanceCustomisePanelInteraction.fontOptions(state, text)

        assertEquals(text.text(GemTextKey.Fonts), options.first().label)
        assertEquals(null, options.first().value)
        assertFalse(options.first().enabled)
        assertEquals(AppearanceFontFamily("Inter"), options[1].value)
        assertEquals(AppearanceFontFamily("Atkinson"), options[2].value)
    }

    @Test
    fun activeTextTargetLabelUsesTargetCatalogueCopy() {
        assertEquals(
            text.text(GemTextKey.AppearanceTextTitleBar),
            AppearanceCustomisePanelInteraction.textTargetLabel(AppearanceTextTarget.TITLE_BAR, text),
        )
    }
}
