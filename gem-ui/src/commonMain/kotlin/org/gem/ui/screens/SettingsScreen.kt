package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.components.ThemeSettingsPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.state.ThemeUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun SettingsScreen(
    themeState: ThemeUiState,
    textCatalogue: GemTextCatalogue,
    onThemeCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.ViewSettings),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        Text(
            text = textCatalogue.text(GemTextKey.Settings),
            style = GemTheme.typeScale.sectionTitle,
            color = GemTheme.colors.secondary,
        )
        ThemeSettingsPanel(
            state = themeState,
            textCatalogue = textCatalogue,
            onCheckedChange = onThemeCheckedChange,
        )
    }
}
