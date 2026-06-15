package org.gem.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.core.domain.AccountProfileId
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AccountsUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun AccountsEditAccountPanel(
    state: AccountsUiState,
    textCatalogue: GemTextCatalogue,
    onToggle: () -> Unit,
    onSavedAccountSelected: (AccountProfileId?) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPanel(modifier = modifier) {
        GemSecondaryButton(
            text = textCatalogue.text(GemTextKey.EditAccount),
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.editAccountExpanded) {
            AccountsErrorText(state)
            SavedLoginDropdown(
                selectedProfileId = state.selectedProfileId,
                options = state.savedLoginOptions,
                enabled = state.savedLoginOptions.isNotEmpty(),
                textCatalogue = textCatalogue,
                onSelected = onSavedAccountSelected,
            )
            GemPasswordField(
                label = textCatalogue.text(GemTextKey.Password),
                value = state.passwordDraft,
                onValueChange = onPasswordChanged,
                revealText = textCatalogue.text(GemTextKey.Show),
                hideText = textCatalogue.text(GemTextKey.Hide),
                revealed = state.passwordVisible,
                onRevealChanged = { onPasswordVisibilityToggle() },
                enabled = state.passwordEnabled,
                modifier = Modifier.testTag(GemTestTags.AccountPassword),
            )
            GemSecondaryButton(
                text = textCatalogue.text(GemTextKey.SavePassword),
                onClick = onSave,
                enabled = state.saveEditedPasswordEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AccountsAddAccountPanel(
    state: AccountsUiState,
    textCatalogue: GemTextCatalogue,
    onToggle: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordFocus: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
) {
    GemPanel(modifier = modifier) {
        if (expandable) {
            GemSecondaryButton(
                text = textCatalogue.text(GemTextKey.AddNewAccount),
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GemTestTags.AddAccount),
            )
        } else {
            GemStaticButtonSurface(
                text = textCatalogue.text(GemTextKey.AddNewAccount),
                modifier = Modifier.testTag(GemTestTags.AddAccount),
            )
        }
        if (state.addAccountExpanded) {
            AccountsErrorText(state)
            GemTextField(
                label = textCatalogue.text(GemTextKey.Username),
                value = state.addUsernameDraft,
                onValueChange = onUsernameChanged,
            )
            GemPasswordField(
                label = textCatalogue.text(GemTextKey.Password),
                value = state.addPasswordDraft,
                onValueChange = onPasswordChanged,
                revealText = textCatalogue.text(GemTextKey.Show),
                hideText = textCatalogue.text(GemTextKey.Hide),
                revealed = state.newPasswordVisible,
                onRevealChanged = { onPasswordVisibilityToggle() },
                onPasswordFocus = onPasswordFocus,
            )
            GemSecondaryButton(
                text = textCatalogue.text(GemTextKey.SaveNewAccount),
                onClick = onSave,
                enabled = state.saveNewAccountEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AccountsErrorText(state: AccountsUiState) {
    val message = state.errorMessage
    if (message != null) {
        Text(
            text = message,
            color = GemTheme.colors.danger,
            style = GemTheme.typeScale.smallLabel,
        )
    }
}
