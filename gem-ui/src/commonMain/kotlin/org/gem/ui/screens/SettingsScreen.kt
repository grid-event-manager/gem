package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.components.AppearancePanelCallbacks
import org.gem.ui.components.AppearanceSettingsPanel
import org.gem.ui.components.LanguagePanelCallbacks
import org.gem.ui.components.LanguageSettingsPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.state.LanguageUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun SettingsScreen(
    appearanceState: AppearanceUiState,
    languageState: LanguageUiState,
    textCatalogue: GemTextCatalogue,
    appearanceCallbacks: AppearancePanelCallbacks,
    languageCallbacks: LanguagePanelCallbacks,
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
            callbacks = appearanceCallbacks,
        )
        LanguageSettingsPanel(
            state = languageState,
            textCatalogue = textCatalogue,
            callbacks = languageCallbacks,
        )
    }
}

internal object SettingsScreenInteraction {
    val cardOrder: List<GemTextKey> = listOf(
        GemTextKey.Customise,
        GemTextKey.Themes,
        GemTextKey.Language,
    )
}
