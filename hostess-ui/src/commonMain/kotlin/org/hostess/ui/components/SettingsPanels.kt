package org.hostess.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
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
fun SettingsSavedAccountPanel(
    state: SettingsUiState,
    textCatalogue: HostessTextCatalogue,
    onSavedAccountSelected: (AccountProfileId?) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        Text(
            text = textCatalogue.text(HostessTextKey.Settings),
            style = HostessTheme.typeScale.sectionTitle,
            color = HostessTheme.colors.ink,
        )
        SettingsErrorText(state)
        SavedLoginDropdown(
            selectedProfileId = state.selectedProfileId,
            options = state.savedLoginOptions,
            enabled = state.savedLoginOptions.isNotEmpty(),
            textCatalogue = textCatalogue,
            onSelected = onSavedAccountSelected,
        )
        HostessPasswordField(
            label = textCatalogue.text(HostessTextKey.Password),
            value = state.passwordDraft,
            onValueChange = onPasswordChanged,
            revealText = textCatalogue.text(HostessTextKey.Show),
            hideText = textCatalogue.text(HostessTextKey.Hide),
            revealed = state.passwordVisible,
            onRevealChanged = { onPasswordVisibilityToggle() },
            enabled = state.passwordEnabled,
            modifier = Modifier.testTag(HostessTestTags.AccountPassword),
        )
    }
}

@Composable
fun SettingsAddAccountPanel(
    state: SettingsUiState,
    textCatalogue: HostessTextCatalogue,
    onToggle: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordFocus: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        HostessSecondaryButton(
            text = textCatalogue.text(HostessTextKey.AddNewAccount),
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.AddAccount),
        )
        if (state.addAccountExpanded) {
            HostessTextField(
                label = textCatalogue.text(HostessTextKey.Username),
                value = state.newUsernameDraft,
                onValueChange = onUsernameChanged,
            )
            HostessPasswordField(
                label = textCatalogue.text(HostessTextKey.Password),
                value = state.newPasswordDraft,
                onValueChange = onPasswordChanged,
                revealText = textCatalogue.text(HostessTextKey.Show),
                hideText = textCatalogue.text(HostessTextKey.Hide),
                revealed = state.newPasswordVisible,
                onRevealChanged = { onPasswordVisibilityToggle() },
                onPasswordFocus = onPasswordFocus,
            )
            HostessSecondaryButton(
                text = textCatalogue.text(HostessTextKey.SaveNewAccount),
                onClick = onSave,
                enabled = state.saveNewAccountEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingsErrorText(state: SettingsUiState) {
    val message = state.errorMessage
    if (message != null) {
        Text(
            text = message,
            color = HostessTheme.colors.danger,
            style = HostessTheme.typeScale.smallLabel,
        )
    }
}
