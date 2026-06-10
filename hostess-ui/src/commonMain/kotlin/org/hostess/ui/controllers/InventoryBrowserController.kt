package org.hostess.ui.controllers

import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.InventoryShortcutUiState

class InventoryBrowserController(
    val runtime: HostessUiRuntime,
    val state: InventoryBrowserUiState = InventoryBrowserUiState(),
) {
    fun openInventoryShortcut(shortcut: InventoryShortcut): InventoryBrowserController {
        // B-09 owns InventoryDirectoryService queries; B-06 stores shortcut state only.
        val shortcuts = when (shortcut) {
            InventoryShortcut.ROOT -> InventoryShortcutUiState(rootSelected = true, landmarksSelected = false)
            InventoryShortcut.LANDMARKS -> InventoryShortcutUiState(landmarksSelected = true)
            InventoryShortcut.TEXTURES -> InventoryShortcutUiState(texturesSelected = true, landmarksSelected = false)
        }
        return copy(state.copy(shortcuts = shortcuts))
    }

    fun openInventoryFolder(folderId: InventoryFolderId): InventoryBrowserController {
        // B-09 owns folder listing refresh; B-06 records the selected folder id.
        return copy(state.copy(currentFolderId = folderId))
    }

    fun selectInventoryAsset(itemId: InventoryItemId): InventoryBrowserController {
        // B-09 owns InventorySelectionService and AttachmentService delegation.
        val rows = state.visibleAssetRows.map { row ->
            row.copy(selected = row.itemId == itemId)
        }
        return copy(state.copy(visibleAssetRows = rows))
    }

    private fun copy(state: InventoryBrowserUiState): InventoryBrowserController =
        InventoryBrowserController(runtime, state)
}
