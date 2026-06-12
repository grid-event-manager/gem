package org.gem.ui.components

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
import org.gem.core.domain.AccountProfileId
import org.gem.ui.design.GemTheme
import org.gem.ui.state.LoginUiState
import org.gem.ui.state.SavedLoginOptionUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun SavedLoginDropdown(
    selectedProfileId: AccountProfileId?,
    options: List<SavedLoginOptionUiState>,
    enabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onSelected: (AccountProfileId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = options.firstOrNull { it.profileId == selectedProfileId }
    val placeholder = textCatalogue.text(GemTextKey.SavedLoginPlaceholder)
    GemDropdownField(
        label = textCatalogue.text(GemTextKey.Username),
        selectedLabel = selected?.loginName,
        placeholderLabel = placeholder,
        options = options.map { option ->
            GemDropdownOption(option.profileId, option.loginName)
        },
        onSelected = onSelected,
        enabled = enabled,
        modifier = modifier,
        fieldModifier = Modifier.testTag(GemTestTags.AccountName),
    )
}

@Composable
fun LoginCredentialPanel(
    state: LoginUiState,
    textCatalogue: GemTextCatalogue,
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
    GemPanel(modifier = modifier) {
        LoginUsernameField(
            usernameDraft = state.usernameDraft,
            selectedProfileId = state.selectedProfileId,
            options = state.savedLoginOptions,
            enabled = controlsEnabled && state.credentialRuntime.ready,
            textCatalogue = textCatalogue,
            onUsernameChanged = onUsernameChanged,
            onSavedLoginSelected = onSavedLoginSelected,
        )
        GemPasswordField(
            label = textCatalogue.text(GemTextKey.Password),
            value = state.passwordDraft,
            onValueChange = onPasswordChanged,
            revealText = textCatalogue.text(GemTextKey.Show),
            hideText = textCatalogue.text(GemTextKey.Hide),
            revealed = state.passwordVisible,
            onRevealChanged = { onPasswordVisibilityToggle() },
            enabled = controlsEnabled && state.passwordEnabled,
            onPasswordFocus = onPasswordFocus,
            modifier = Modifier.testTag(GemTestTags.AccountPassword),
        )
        LoginOperationText(state, textCatalogue)
        GemSecondaryButton(
            text = textCatalogue.text(GemTextKey.Login),
            onClick = onLogin,
            enabled = controlsEnabled && state.loginEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GemTestTags.LoginButton),
        )
    }
}

@Composable
private fun LoginUsernameField(
    usernameDraft: String,
    selectedProfileId: AccountProfileId?,
    options: List<SavedLoginOptionUiState>,
    enabled: Boolean,
    textCatalogue: GemTextCatalogue,
    onUsernameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSavedLoginSelected: (AccountProfileId?) -> Unit,
) {
    GemDropdownTextField(
        label = textCatalogue.text(GemTextKey.Username),
        value = usernameDraft,
        onValueChange = onUsernameChanged,
        placeholderLabel = textCatalogue.text(GemTextKey.SavedLoginPlaceholder),
        options = options.map { option ->
            GemDropdownOption(option.profileId, option.loginName)
        },
        onSelected = onSavedLoginSelected,
        enabled = enabled,
        modifier = modifier,
        fieldModifier = Modifier.testTag(GemTestTags.AccountName),
    )
}

@Composable
private fun LoginOperationText(
    state: LoginUiState,
    textCatalogue: GemTextCatalogue,
) {
    val operation = state.operation
    if (operation.inFlight) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(GemTheme.spacing.statusPillMinHeight),
                strokeWidth = GemTheme.spacing.borderWidth,
                color = GemTheme.colors.primary,
            )
            Text(
                text = textCatalogue.text(operation.messageKey ?: GemTextKey.LoggingIn),
                color = GemTheme.colors.muted,
                style = GemTheme.typeScale.smallLabel,
            )
        }
    }
    if (operation.errorKey != null || operation.errorMessage != null) {
        val message = listOfNotNull(
            operation.errorKey
                ?.takeUnless { it == GemTextKey.BlankStatus }
                ?.let(textCatalogue::text),
            operation.errorMessage,
        ).joinToString(": ")
        Text(
            text = message,
            color = GemTheme.colors.danger,
            style = GemTheme.typeScale.smallLabel,
        )
    }
}
