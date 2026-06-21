package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemId
import org.gem.ui.components.GroupTargetSelector
import org.gem.ui.components.InventoryBrowser
import org.gem.ui.components.NoticeEditor
import org.gem.ui.design.GemTheme
import org.gem.ui.state.GroupTargetUiState
import org.gem.ui.state.InventoryBrowserUiState
import org.gem.ui.state.InventoryShortcut
import org.gem.ui.state.NoticeComposerUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
fun ComposeScreen(
    noticeState: NoticeComposerUiState = NoticeComposerUiState(),
    inventoryState: InventoryBrowserUiState = InventoryBrowserUiState(),
    groupTargetState: GroupTargetUiState = GroupTargetUiState(),
    textCatalogue: GemTextCatalogue,
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
            .testTag(GemTestTags.ViewCompose),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
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
