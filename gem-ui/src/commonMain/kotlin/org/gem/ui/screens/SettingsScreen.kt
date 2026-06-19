package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.components.AppearancePanelCallbacks
import org.gem.ui.components.AppearanceSettingsPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
fun SettingsScreen(
    appearanceState: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
    callbacks: AppearancePanelCallbacks,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.ViewSettings),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        AppearanceSettingsPanel(
            state = appearanceState,
            textCatalogue = textCatalogue,
            callbacks = callbacks,
        )
    }
}
