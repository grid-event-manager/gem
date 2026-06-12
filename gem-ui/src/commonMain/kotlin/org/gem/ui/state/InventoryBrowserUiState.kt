package org.gem.ui.state

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemKind
import org.gem.ui.text.GemTextKey

data class InventoryBrowserUiState(
    val loading: Boolean = true,
    val currentFolderId: InventoryFolderId? = null,
    val currentPath: List<String> = emptyList(),
    val shortcuts: InventoryShortcutUiState = InventoryShortcutUiState(),
    val visibleFolderRows: List<InventoryFolderRowUiState> = emptyList(),
    val visibleAssetRows: List<InventoryAssetRowUiState> = emptyList(),
    val selectedAttachment: SelectedAttachmentUiState? = null,
    val attachmentSummary: GemTextKey = GemTextKey.None,
    val errorKey: GemTextKey? = null,
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
