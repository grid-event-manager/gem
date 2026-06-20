package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AppearanceExpandedPanel
import org.gem.ui.state.AppearanceRgbChannel
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

data class AppearancePanelCallbacks(
    val onExpandedPanelChanged: (AppearanceExpandedPanel) -> Unit,
    val onTextTargetSelectorOpened: () -> Unit,
    val onElementTargetSelectorOpened: () -> Unit,
    val onTextTargetSelected: (AppearanceTextTarget) -> Unit,
    val onElementTargetSelected: (AppearanceElementTarget) -> Unit,
    val onFontSelected: (AppearanceFontFamily) -> Unit,
    val onColorSelected: (AppearanceColor) -> Unit,
    val onRgbValueChanged: (AppearanceRgbChannel, Int) -> Unit,
    val onRgbInputInvalid: (AppearanceRgbChannel) -> Unit,
    val onHexChanged: (String) -> Unit,
    val onOpenSaveThemeDialog: () -> Unit,
    val onCloseSaveThemeDialog: () -> Unit,
    val onSaveThemeNameChanged: (String) -> Unit,
    val onSaveThemeModeChanged: (AppearanceMode) -> Unit,
    val onSaveTheme: (String, AppearanceMode) -> Unit,
    val onResetCurrentMode: () -> Unit,
    val onProfileSelected: (AppearanceProfileId) -> Unit,
)

@Composable
fun AppearanceSettingsPanel(
    state: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
    callbacks: AppearancePanelCallbacks,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        GemExpandablePanelHeader(
            text = textCatalogue.text(GemTextKey.Customise),
            expanded = state.expandedPanel == AppearanceExpandedPanel.CUSTOMISE,
            enabled = enabled,
            onClick = {
                callbacks.onExpandedPanelChanged(
                    AppearanceSettingsPanelInteraction.nextExpandedPanel(
                        current = state.expandedPanel,
                        selected = AppearanceExpandedPanel.CUSTOMISE,
                    ),
                )
            },
        )
        if (state.expandedPanel == AppearanceExpandedPanel.CUSTOMISE) {
            GemPanel {
                AppearanceCustomisePanel(
                    state = state,
                    textCatalogue = textCatalogue,
                    callbacks = callbacks,
                    enabled = enabled,
                )
            }
        }

        GemExpandablePanelHeader(
            text = textCatalogue.text(GemTextKey.Themes),
            expanded = state.expandedPanel == AppearanceExpandedPanel.THEMES,
            enabled = enabled,
            onClick = {
                callbacks.onExpandedPanelChanged(
                    AppearanceSettingsPanelInteraction.nextExpandedPanel(
                        current = state.expandedPanel,
                        selected = AppearanceExpandedPanel.THEMES,
                    ),
                )
            },
        )
        if (state.expandedPanel == AppearanceExpandedPanel.THEMES) {
            GemPanel {
                AppearanceThemesPanel(
                    state = state,
                    textCatalogue = textCatalogue,
                    onProfileSelected = callbacks.onProfileSelected,
                    enabled = enabled,
                )
            }
        }

        AppearanceSettingsPanelInteraction.errorKey(state)?.let { errorKey ->
            Text(
                text = textCatalogue.text(errorKey),
                color = GemTheme.colors.danger,
                style = GemTheme.typeScale.smallLabel,
            )
        }
    }
}

internal object AppearanceSettingsPanelInteraction {
    val headerOrder: List<AppearanceExpandedPanel> = listOf(
        AppearanceExpandedPanel.CUSTOMISE,
        AppearanceExpandedPanel.THEMES,
    )

    fun nextExpandedPanel(
        current: AppearanceExpandedPanel,
        selected: AppearanceExpandedPanel,
    ): AppearanceExpandedPanel =
        if (current == selected) AppearanceExpandedPanel.NONE else selected

    fun errorKey(state: AppearanceUiState): GemTextKey? =
        state.errorKey ?: state.profileWarning?.let { GemTextKey.ThemePreferenceUnavailable }
}
