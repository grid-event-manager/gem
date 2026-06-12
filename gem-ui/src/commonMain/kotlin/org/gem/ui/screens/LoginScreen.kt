package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.core.domain.AccountProfileId
import org.gem.ui.components.LoginCredentialPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.state.LoginUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
fun LoginScreen(
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.ViewLogin),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
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
