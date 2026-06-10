package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SavedLoginOptionUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun SavedLoginDropdown(
    selectedProfileId: AccountProfileId?,
    options: List<SavedLoginOptionUiState>,
    enabled: Boolean,
    textCatalogue: HostessTextCatalogue,
    onSelected: (AccountProfileId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.profileId == selectedProfileId }
    val placeholder = textCatalogue.text(HostessTextKey.SavedLoginPlaceholder)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
    ) {
        Text(
            text = textCatalogue.text(HostessTextKey.Username),
            style = HostessTheme.typeScale.smallLabel,
            color = HostessTheme.colors.muted,
        )
        HostessSecondaryButton(
            text = selected?.loginName ?: placeholder,
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.AccountName),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(placeholder, style = HostessTheme.typeScale.body) },
                onClick = {
                    expanded = false
                    onSelected(null)
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.loginName, style = HostessTheme.typeScale.body) },
                    onClick = {
                        expanded = false
                        onSelected(option.profileId)
                    },
                )
            }
        }
    }
}

@Composable
fun LoginSavedAccountPanel(
    selectedProfileId: AccountProfileId?,
    options: List<SavedLoginOptionUiState>,
    passwordDraft: String,
    passwordVisible: Boolean,
    passwordEnabled: Boolean,
    loginEnabled: Boolean,
    textCatalogue: HostessTextCatalogue,
    onSavedLoginSelected: (AccountProfileId?) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        SavedLoginDropdown(
            selectedProfileId = selectedProfileId,
            options = options,
            enabled = options.isNotEmpty(),
            textCatalogue = textCatalogue,
            onSelected = onSavedLoginSelected,
        )
        HostessPasswordField(
            label = textCatalogue.text(HostessTextKey.Password),
            value = passwordDraft,
            onValueChange = onPasswordChanged,
            revealText = textCatalogue.text(HostessTextKey.Show),
            hideText = textCatalogue.text(HostessTextKey.Hide),
            revealed = passwordVisible,
            onRevealChanged = { onPasswordVisibilityToggle() },
            enabled = passwordEnabled,
            modifier = Modifier.testTag(HostessTestTags.AccountPassword),
        )
        HostessSecondaryButton(
            text = textCatalogue.text(HostessTextKey.Login),
            onClick = onLogin,
            enabled = loginEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.LoginButton),
        )
    }
}

@Composable
fun AddLoginPanel(
    expanded: Boolean,
    usernameDraft: String,
    passwordDraft: String,
    passwordVisible: Boolean,
    saveAndLoginEnabled: Boolean,
    textCatalogue: HostessTextCatalogue,
    onToggle: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordFocus: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSaveAndLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        HostessSecondaryButton(
            text = textCatalogue.text(HostessTextKey.AddNewLogin),
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.AddAccount),
        )
        if (expanded) {
            HostessTextField(
                label = textCatalogue.text(HostessTextKey.Username),
                value = usernameDraft,
                onValueChange = onUsernameChanged,
            )
            HostessPasswordField(
                label = textCatalogue.text(HostessTextKey.Password),
                value = passwordDraft,
                onValueChange = onPasswordChanged,
                revealText = textCatalogue.text(HostessTextKey.Show),
                hideText = textCatalogue.text(HostessTextKey.Hide),
                revealed = passwordVisible,
                onRevealChanged = { onPasswordVisibilityToggle() },
                onPasswordFocus = onPasswordFocus,
            )
            HostessSecondaryButton(
                text = textCatalogue.text(HostessTextKey.SaveAndLogin),
                onClick = onSaveAndLogin,
                enabled = saveAndLoginEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
