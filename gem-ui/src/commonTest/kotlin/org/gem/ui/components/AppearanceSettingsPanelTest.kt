package org.gem.ui.components

import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.testing.controllerBackedAppearanceState
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsPanelTest {
    @Test
    fun defaultStateHasBothCardsCollapsed() {
        val state = controllerBackedAppearanceState(osDark = false)

        assertEquals(AppearanceExpandedPanel.NONE, state.expandedPanel)
        assertEquals(
            listOf(AppearanceExpandedPanel.CUSTOMISE),
            AppearanceSettingsPanelInteraction.headerOrder,
        )
    }

    @Test
    fun openingCustomisePanelCanCollapse() {
        assertEquals(
            AppearanceExpandedPanel.CUSTOMISE,
            AppearanceSettingsPanelInteraction.nextExpandedPanel(
                current = AppearanceExpandedPanel.NONE,
                selected = AppearanceExpandedPanel.CUSTOMISE,
            ),
        )
        assertEquals(
            AppearanceExpandedPanel.NONE,
            AppearanceSettingsPanelInteraction.nextExpandedPanel(
                current = AppearanceExpandedPanel.CUSTOMISE,
                selected = AppearanceExpandedPanel.CUSTOMISE,
            ),
        )
    }

    @Test
    fun warningUsesCentralErrorCopy() {
        val state = controllerBackedAppearanceState(osDark = false)

        assertEquals(null, AppearanceSettingsPanelInteraction.errorKey(state))
        assertEquals(GemTextKey.ThemePreferenceSaveFailed, AppearanceSettingsPanelInteraction.errorKey(
            state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
        ))
    }
}
