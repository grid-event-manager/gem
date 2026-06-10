package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import org.hostess.ui.state.LoginUiState
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
fun LoginCredentialPanel(
    state: LoginUiState,
    textCatalogue: HostessTextCatalogue,
    onSavedLoginSelected: (AccountProfileId?) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordFocus: () -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val operation = state.operation
    val controlsEnabled = !operation.inFlight
    HostessPanel(modifier = modifier) {
        LoginUsernameField(
            usernameDraft = state.usernameDraft,
            selectedProfileId = state.selectedProfileId,
            options = state.savedLoginOptions,
            enabled = controlsEnabled && state.credentialRuntime.ready,
            textCatalogue = textCatalogue,
            onUsernameChanged = onUsernameChanged,
            onSavedLoginSelected = onSavedLoginSelected,
        )
        HostessPasswordField(
            label = textCatalogue.text(HostessTextKey.Password),
            value = state.passwordDraft,
            onValueChange = onPasswordChanged,
            revealText = textCatalogue.text(HostessTextKey.Show),
            hideText = textCatalogue.text(HostessTextKey.Hide),
            revealed = state.passwordVisible,
            onRevealChanged = { onPasswordVisibilityToggle() },
            enabled = controlsEnabled && state.passwordEnabled,
            onPasswordFocus = onPasswordFocus,
            modifier = Modifier.testTag(HostessTestTags.AccountPassword),
        )
        LoginOperationText(state, textCatalogue)
        HostessSecondaryButton(
            text = textCatalogue.text(HostessTextKey.Login),
            onClick = onLogin,
            enabled = controlsEnabled && state.loginEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.LoginButton),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginUsernameField(
    usernameDraft: String,
    selectedProfileId: AccountProfileId?,
    options: List<SavedLoginOptionUiState>,
    enabled: Boolean,
    textCatalogue: HostessTextCatalogue,
    onUsernameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSavedLoginSelected: (AccountProfileId?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
    ) {
        HostessFieldLabel(textCatalogue.text(HostessTextKey.Username))
        ExposedDropdownMenuBox(
            expanded = expanded && options.isNotEmpty(),
            onExpandedChange = {
                if (enabled && options.isNotEmpty()) {
                    expanded = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = usernameDraft,
                onValueChange = onUsernameChanged,
                enabled = enabled,
                singleLine = true,
                textStyle = HostessTheme.typeScale.body,
                shape = HostessTheme.shapes.control,
                colors = hostessTextFieldColors(),
                trailingIcon = {
                    if (options.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled)
                    .fillMaxWidth()
                    .testTag(HostessTestTags.AccountName),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(textCatalogue.text(HostessTextKey.SavedLoginPlaceholder)) },
                    onClick = {
                        expanded = false
                        onSavedLoginSelected(null)
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.loginName, style = HostessTheme.typeScale.body) },
                        onClick = {
                            expanded = false
                            onSavedLoginSelected(option.profileId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginOperationText(
    state: LoginUiState,
    textCatalogue: HostessTextCatalogue,
) {
    val operation = state.operation
    if (operation.inFlight) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(HostessTheme.spacing.statusPillMinHeight),
                strokeWidth = HostessTheme.spacing.borderWidth,
                color = HostessTheme.colors.primary,
            )
            Text(
                text = textCatalogue.text(operation.messageKey ?: HostessTextKey.LoggingIn),
                color = HostessTheme.colors.muted,
                style = HostessTheme.typeScale.smallLabel,
            )
        }
    }
    if (operation.errorKey != null || operation.errorMessage != null) {
        val message = listOfNotNull(
            operation.errorKey
                ?.takeUnless { it == HostessTextKey.BlankStatus }
                ?.let(textCatalogue::text),
            operation.errorMessage,
        ).joinToString(": ")
        Text(
            text = message,
            color = HostessTheme.colors.danger,
            style = HostessTheme.typeScale.smallLabel,
        )
    }
}
