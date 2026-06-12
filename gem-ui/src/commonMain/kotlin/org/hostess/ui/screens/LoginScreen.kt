package org.hostess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.components.LoginCredentialPanel
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.LoginUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun LoginScreen(
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.ViewLogin),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
    ) {
        LoginCredentialPanel(
            state = state,
            textCatalogue = textCatalogue,
            onSavedLoginSelected = onSavedLoginSelected,
            onUsernameChanged = onUsernameChanged,
            onPasswordFocus = onPasswordFocus,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            onPasswordChanged = onPasswordChanged,
            onLogin = onLogin,
        )
    }
}
