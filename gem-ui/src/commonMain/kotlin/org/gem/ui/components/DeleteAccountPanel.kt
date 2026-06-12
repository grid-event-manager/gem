package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.core.domain.AccountProfileId
import org.gem.ui.design.GemTheme
import org.gem.ui.state.SettingsUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun DeleteAccountPanel(
    state: SettingsUiState,
    textCatalogue: GemTextCatalogue,
    onAction: () -> Unit,
    onAccountSelected: (AccountProfileId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPanel(
        modifier = modifier.testTag(GemTestTags.DeleteAccountPanel),
    ) {
        GemSecondaryButton(
            text = if (state.deleteExpanded && state.deleteEnabled) {
                textCatalogue.text(GemTextKey.Delete)
            } else {
                textCatalogue.text(GemTextKey.DeleteAccount)
            },
            onClick = onAction,
            danger = state.deleteExpanded && state.deleteEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GemTestTags.DeleteAccount),
        )
        if (state.deleteExpanded) {
            GemScrollablePane(
                minHeight = GemTheme.spacing.controlHeight,
                maxHeight = GemTheme.spacing.scrollListMaxHeight,
                testTag = GemTestTags.DeleteAccountList,
                verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
            ) {
                state.savedLoginOptions.forEach { option ->
                    GemCheckboxCard(
                        text = option.loginName,
                        checked = option.profileId in state.selectedDeleteProfileIds,
                        onCheckedChange = { selected -> onAccountSelected(option.profileId, selected) },
                    )
                }
            }
        }
    }
}
