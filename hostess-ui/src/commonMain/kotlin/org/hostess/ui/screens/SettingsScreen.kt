package org.hostess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.components.DeleteAccountPanel
import org.hostess.ui.components.HostessConfirmModal
import org.hostess.ui.components.SessionStrip
import org.hostess.ui.components.SettingsAddAccountPanel
import org.hostess.ui.components.SettingsBackNav
import org.hostess.ui.components.SettingsSavedAccountPanel
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.state.SettingsUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    sessionStripState: SessionStripUiState,
    textCatalogue: HostessTextCatalogue,
    onBack: () -> Unit,
    onSavedAccountSelected: (AccountProfileId?) -> Unit,
    onSavedPasswordVisibilityToggle: () -> Unit,
    onSavedPasswordChanged: (String) -> Unit,
    onAddAccountToggle: () -> Unit,
    onNewUsernameChanged: (String) -> Unit,
    onNewPasswordFocus: () -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onNewPasswordVisibilityToggle: () -> Unit,
    onSaveNewAccount: () -> Unit,
    onDeleteAccountToggle: () -> Unit,
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
        SettingsBackNav(
            text = textCatalogue.text(HostessTextKey.Back),
            onBack = onBack,
        )
        SessionStrip(
            state = sessionStripState,
            textCatalogue = textCatalogue,
        )
        SettingsSavedAccountPanel(
            state = state,
            textCatalogue = textCatalogue,
            onSavedAccountSelected = onSavedAccountSelected,
            onPasswordVisibilityToggle = onSavedPasswordVisibilityToggle,
            onPasswordChanged = onSavedPasswordChanged,
        )
        SettingsAddAccountPanel(
            state = state,
            textCatalogue = textCatalogue,
            onToggle = onAddAccountToggle,
            onUsernameChanged = onNewUsernameChanged,
            onPasswordFocus = onNewPasswordFocus,
            onPasswordChanged = onNewPasswordChanged,
            onPasswordVisibilityToggle = onNewPasswordVisibilityToggle,
            onSave = onSaveNewAccount,
        )
        DeleteAccountPanel(
            state = state,
            textCatalogue = textCatalogue,
            onToggle = onDeleteAccountToggle,
            onAccountSelected = onDeleteAccountSelected,
            onDelete = onOpenDeleteModal,
        )
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
