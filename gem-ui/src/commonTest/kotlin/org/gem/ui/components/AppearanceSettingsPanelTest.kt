package org.gem.ui.components

import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsPanelTest {
    @Test
    fun defaultStateHasBothCardsCollapsed() {
        val state = AppearanceUiState.default(osDark = false)

        assertEquals(AppearanceExpandedPanel.NONE, state.expandedPanel)
        assertEquals(
            listOf(AppearanceExpandedPanel.CUSTOMISE, AppearanceExpandedPanel.THEMES),
            AppearanceSettingsPanelInteraction.headerOrder,
        )
    }

    @Test
    fun openingOnePanelClosesTheOtherAndOpenPanelCanCollapse() {
        assertEquals(
            AppearanceExpandedPanel.CUSTOMISE,
            AppearanceSettingsPanelInteraction.nextExpandedPanel(
                current = AppearanceExpandedPanel.NONE,
                selected = AppearanceExpandedPanel.CUSTOMISE,
            ),
        )
        assertEquals(
            AppearanceExpandedPanel.THEMES,
            AppearanceSettingsPanelInteraction.nextExpandedPanel(
                current = AppearanceExpandedPanel.CUSTOMISE,
                selected = AppearanceExpandedPanel.THEMES,
            ),
        )
        assertEquals(
            AppearanceExpandedPanel.NONE,
            AppearanceSettingsPanelInteraction.nextExpandedPanel(
                current = AppearanceExpandedPanel.THEMES,
                selected = AppearanceExpandedPanel.THEMES,
            ),
        )
    }

    @Test
    fun warningUsesCentralErrorCopy() {
        val state = AppearanceUiState.default(osDark = false)

        assertEquals(null, AppearanceSettingsPanelInteraction.errorKey(state))
        assertEquals(GemTextKey.ThemePreferenceSaveFailed, AppearanceSettingsPanelInteraction.errorKey(
            state.copy(errorKey = GemTextKey.ThemePreferenceSaveFailed),
        ))
    }
}
