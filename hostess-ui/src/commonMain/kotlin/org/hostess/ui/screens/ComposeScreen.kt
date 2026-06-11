package org.hostess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.ui.components.GroupTargetSelector
import org.hostess.ui.components.InventoryBrowser
import org.hostess.ui.components.NoticeEditor
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.NoticeComposerUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun ComposeScreen(
    noticeState: NoticeComposerUiState = NoticeComposerUiState(),
    inventoryState: InventoryBrowserUiState = InventoryBrowserUiState(),
    groupTargetState: GroupTargetUiState = GroupTargetUiState(),
    textCatalogue: HostessTextCatalogue = EnglishHostessTextCatalogue,
    onSubjectChanged: (String) -> Unit = {},
    onBodyChanged: (String) -> Unit = {},
    onInventoryShortcutSelected: (InventoryShortcut) -> Unit = {},
    onInventoryFolderOpen: (InventoryFolderId) -> Unit = {},
    onInventoryAssetSelected: (InventoryItemId) -> Unit = {},
    onAttachmentCleared: () -> Unit = {},
    onAllGroupsChanged: (Boolean) -> Unit = {},
    onManualGroupsChanged: (Boolean) -> Unit = {},
    onManualGroupSelected: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.ViewCompose),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.rowGap),
    ) {
        NoticeEditor(
            state = noticeState,
            textCatalogue = textCatalogue,
            onSubjectChanged = onSubjectChanged,
            onBodyChanged = onBodyChanged,
        )
        InventoryBrowser(
            state = inventoryState,
            textCatalogue = textCatalogue,
            onShortcutSelected = onInventoryShortcutSelected,
            onFolderOpen = onInventoryFolderOpen,
            onAssetSelected = onInventoryAssetSelected,
            onAttachmentCleared = onAttachmentCleared,
        )
        GroupTargetSelector(
            state = groupTargetState,
            textCatalogue = textCatalogue,
            onAllGroupsChanged = onAllGroupsChanged,
            onManualGroupsChanged = onManualGroupsChanged,
            onManualGroupSelected = onManualGroupSelected,
        )
    }
}
