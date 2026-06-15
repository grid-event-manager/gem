package org.gem.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.ui.design.GemTheme
import org.gem.ui.state.ThemeUiState
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun ThemeSettingsPanel(
    state: ThemeUiState,
    textCatalogue: GemTextCatalogue,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(GemTheme.spacing.fieldGap),
    ) {
        ThemeModeToggle(
            checked = state.toggleChecked,
            lightLabel = textCatalogue.text(GemTextKey.Light),
            darkLabel = textCatalogue.text(GemTextKey.Dark),
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        state.errorKey?.let { errorKey ->
            Text(
                text = textCatalogue.text(errorKey),
                color = GemTheme.colors.danger,
                style = GemTheme.typeScale.smallLabel,
            )
        }
    }
}
