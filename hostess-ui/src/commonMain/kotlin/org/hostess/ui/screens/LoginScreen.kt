package org.hostess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.components.AddLoginPanel
import org.hostess.ui.components.LoginSavedAccountPanel
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.LoginUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun LoginScreen(
    state: LoginUiState,
    textCatalogue: HostessTextCatalogue,
    onSavedLoginSelected: (AccountProfileId?) -> Unit,
    onSavedPasswordVisibilityToggle: () -> Unit,
    onSavedPasswordChanged: (String) -> Unit,
    onAddLoginToggle: () -> Unit,
    onNewUsernameChanged: (String) -> Unit,
    onNewPasswordFocus: () -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onNewPasswordVisibilityToggle: () -> Unit,
    onLogin: () -> Unit,
    onSaveAndLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.ViewLogin),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
    ) {
        LoginSavedAccountPanel(
            selectedProfileId = state.selectedProfileId,
            options = state.savedLoginOptions,
            passwordDraft = state.passwordDraft,
            passwordVisible = state.passwordVisible,
            passwordEnabled = state.passwordEnabled,
            loginEnabled = state.loginEnabled,
            textCatalogue = textCatalogue,
            onSavedLoginSelected = onSavedLoginSelected,
            onPasswordVisibilityToggle = onSavedPasswordVisibilityToggle,
            onPasswordChanged = onSavedPasswordChanged,
            onLogin = onLogin,
        )
        AddLoginPanel(
            expanded = state.addLoginExpanded,
            usernameDraft = state.newUsernameDraft,
            passwordDraft = state.newPasswordDraft,
            passwordVisible = state.newPasswordVisible,
            saveAndLoginEnabled = state.saveAndLoginEnabled,
            textCatalogue = textCatalogue,
            onToggle = onAddLoginToggle,
            onUsernameChanged = onNewUsernameChanged,
            onPasswordFocus = onNewPasswordFocus,
            onPasswordChanged = onNewPasswordChanged,
            onPasswordVisibilityToggle = onNewPasswordVisibilityToggle,
            onSaveAndLogin = onSaveAndLogin,
        )
    }
}
