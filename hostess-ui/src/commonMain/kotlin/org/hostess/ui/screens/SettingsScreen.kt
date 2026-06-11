package org.hostess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.components.DeleteAccountPanel
import org.hostess.ui.components.HostessConfirmModal
import org.hostess.ui.components.SettingsAddAccountPanel
import org.hostess.ui.components.SettingsEditAccountPanel
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SettingsUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    textCatalogue: HostessTextCatalogue,
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
            .testTag(HostessTestTags.ViewSettings),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
    ) {
        val hasSavedAccounts = state.savedLoginOptions.isNotEmpty()
        Text(
            text = textCatalogue.text(HostessTextKey.Settings),
            style = HostessTheme.typeScale.sectionTitle,
            color = HostessTheme.colors.secondary,
        )
        if (hasSavedAccounts) {
            SettingsEditAccountPanel(
                state = state,
                textCatalogue = textCatalogue,
                onToggle = onEditAccountToggle,
                onSavedAccountSelected = onSavedAccountSelected,
                onPasswordVisibilityToggle = onSavedPasswordVisibilityToggle,
                onPasswordChanged = onSavedPasswordChanged,
                onSave = onSaveEditedPassword,
            )
        }
        SettingsAddAccountPanel(
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
        HostessConfirmModal(
            visible = state.confirmDeleteOpen,
            title = textCatalogue.text(HostessTextKey.DeleteConfirmation),
            confirmText = textCatalogue.text(HostessTextKey.Ok),
            cancelText = textCatalogue.text(HostessTextKey.Cancel),
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete,
            modifier = Modifier.testTag(HostessTestTags.DeleteModal),
            confirmModifier = Modifier.testTag(HostessTestTags.ConfirmDelete),
            cancelModifier = Modifier.testTag(HostessTestTags.CancelDelete),
        )
    }
}
