package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.core.domain.AccountProfileId
import org.gem.ui.components.DeleteAccountPanel
import org.gem.ui.components.GemConfirmModal
import org.gem.ui.components.AccountsAddAccountPanel
import org.gem.ui.components.AccountsEditAccountPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AccountsUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    textCatalogue: GemTextCatalogue,
    onEditAccountToggle: () -> Unit,
    onSavedAccountSelected: (AccountProfileId?) -> Unit,
    onSavedPasswordVisibilityToggle: () -> Unit,
    onSavedPasswordChanged: (String) -> Unit,
    onSaveEditedPassword: () -> Unit,
    onAddAccountToggle: () -> Unit,
    onNewUsernameChanged: (String) -> Unit,
    onNewPasswordFocus: () -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onNewPasswordVisibilityToggle: () -> Unit,
    onSaveNewAccount: () -> Unit,
    onDeleteAccountSelected: (AccountProfileId, Boolean) -> Unit,
    onOpenDeleteModal: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.ViewAccounts),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        val hasSavedAccounts = state.savedLoginOptions.isNotEmpty()
        Text(
            text = textCatalogue.text(GemTextKey.Accounts),
            style = GemTheme.typeScale.sectionTitle,
            color = GemTheme.colors.secondary,
        )
        if (hasSavedAccounts) {
            AccountsEditAccountPanel(
                state = state,
                textCatalogue = textCatalogue,
                onToggle = onEditAccountToggle,
                onSavedAccountSelected = onSavedAccountSelected,
                onPasswordVisibilityToggle = onSavedPasswordVisibilityToggle,
                onPasswordChanged = onSavedPasswordChanged,
                onSave = onSaveEditedPassword,
            )
        }
        AccountsAddAccountPanel(
            state = if (hasSavedAccounts) state else state.copy(addAccountExpanded = true),
            textCatalogue = textCatalogue,
            onToggle = onAddAccountToggle,
            onUsernameChanged = onNewUsernameChanged,
            onPasswordFocus = onNewPasswordFocus,
            onPasswordChanged = onNewPasswordChanged,
            onPasswordVisibilityToggle = onNewPasswordVisibilityToggle,
            onSave = onSaveNewAccount,
            expandable = hasSavedAccounts,
        )
        if (hasSavedAccounts) {
            DeleteAccountPanel(
                state = state,
                textCatalogue = textCatalogue,
                onAction = onOpenDeleteModal,
                onAccountSelected = onDeleteAccountSelected,
            )
        }
        GemConfirmModal(
            visible = state.confirmDeleteOpen,
            title = textCatalogue.text(GemTextKey.DeleteConfirmation),
            confirmText = textCatalogue.text(GemTextKey.Ok),
            cancelText = textCatalogue.text(GemTextKey.Cancel),
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete,
            modifier = Modifier.testTag(GemTestTags.DeleteModal),
            confirmModifier = Modifier.testTag(GemTestTags.ConfirmDelete),
            cancelModifier = Modifier.testTag(GemTestTags.CancelDelete),
        )
    }
}
