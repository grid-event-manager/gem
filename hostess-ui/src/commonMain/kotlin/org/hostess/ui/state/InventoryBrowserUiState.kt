package org.hostess.ui.state

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.ui.text.HostessTextKey

data class InventoryBrowserUiState(
    val currentFolderId: InventoryFolderId? = null,
    val currentPath: List<String> = emptyList(),
    val shortcuts: InventoryShortcutUiState = InventoryShortcutUiState(),
    val visibleFolderRows: List<InventoryFolderRowUiState> = emptyList(),
    val visibleAssetRows: List<InventoryAssetRowUiState> = emptyList(),
    val selectedAttachment: SelectedAttachmentUiState? = null,
    val attachmentSummary: HostessTextKey = HostessTextKey.None,
    val errorKey: HostessTextKey? = null,
)

data class InventoryShortcutUiState(
    val rootSelected: Boolean = false,
    val landmarksSelected: Boolean = true,
    val texturesSelected: Boolean = false,
)

data class InventoryFolderRowUiState(
    val folderId: InventoryFolderId,
    val displayName: String,
)

data class InventoryAssetRowUiState(
    val itemId: InventoryItemId,
    val displayName: String,
    val kind: InventoryItemKind,
    val selected: Boolean = false,
)

data class SelectedAttachmentUiState(
    val itemId: InventoryItemId,
    val displayName: String,
    val kind: InventoryItemKind,
    val request: ExistingInventoryAttachment,
    val attachmentRef: AttachmentRef,
)
