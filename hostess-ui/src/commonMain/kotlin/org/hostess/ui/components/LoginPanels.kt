package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val selected = options.firstOrNull { it.profileId == selectedProfileId }
    val placeholder = textCatalogue.text(HostessTextKey.SavedLoginPlaceholder)
    HostessDropdownField(
        label = textCatalogue.text(HostessTextKey.Username),
        selectedLabel = selected?.loginName,
        placeholderLabel = placeholder,
        options = options.map { option ->
            HostessDropdownOption(option.profileId, option.loginName)
        },
        onSelected = onSelected,
        enabled = enabled,
        modifier = modifier,
        fieldModifier = Modifier.testTag(HostessTestTags.AccountName),
    )
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
    HostessDropdownTextField(
        label = textCatalogue.text(HostessTextKey.Username),
        value = usernameDraft,
        onValueChange = onUsernameChanged,
        placeholderLabel = textCatalogue.text(HostessTextKey.SavedLoginPlaceholder),
        options = options.map { option ->
            HostessDropdownOption(option.profileId, option.loginName)
        },
        onSelected = onSavedLoginSelected,
        enabled = enabled,
        modifier = modifier,
        fieldModifier = Modifier.testTag(HostessTestTags.AccountName),
    )
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
