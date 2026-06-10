package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SettingsUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun DeleteAccountPanel(
    state: SettingsUiState,
    textCatalogue: HostessTextCatalogue,
    onToggle: () -> Unit,
    onAccountSelected: (AccountProfileId, Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(
        modifier = modifier.testTag(HostessTestTags.DeleteAccountPanel),
    ) {
        HostessSecondaryButton(
            text = textCatalogue.text(HostessTextKey.DeleteAccount),
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.deleteExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = HostessTheme.spacing.scrollListMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .testTag(HostessTestTags.DeleteAccountList),
                verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
            ) {
                state.savedLoginOptions.forEach { option ->
                    HostessCheckboxCard(
                        text = option.loginName,
                        checked = option.profileId in state.selectedDeleteProfileIds,
                        onCheckedChange = { selected -> onAccountSelected(option.profileId, selected) },
                    )
                }
            }
            HostessSecondaryButton(
                text = textCatalogue.text(HostessTextKey.Delete),
                onClick = onDelete,
                enabled = state.deleteEnabled,
                danger = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HostessTestTags.DeleteAccount),
            )
        }
    }
}
