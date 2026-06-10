package org.hostess.ui.controllers

import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryAttachmentSelectionResult
import org.hostess.core.domain.InventoryDirectoryListing
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryFolderDescriptor
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemDisplayName
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryDirectoryListResult
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.state.InventoryAssetRowUiState
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryFolderRowUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.InventoryShortcutUiState
import org.hostess.ui.state.SelectedAttachmentUiState
import org.hostess.ui.text.HostessTextKey

class InventoryBrowserController(
    val runtime: HostessUiRuntime,
    val session: HostessSession? = null,
    val state: InventoryBrowserUiState = InventoryBrowserUiState(),
    private val listing: InventoryDirectoryListing? = null,
) {
    fun refreshInventory(): InventoryBrowserController {
        val activeSession = session ?: return errorState()
        val query = InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK, InventoryItemKind.TEXTURE))
        return when (val result = runtime.inventoryDirectoryService.listDirectory(activeSession, query)) {
            is InventoryDirectoryListResult.Success -> {
                val root = rootFolder(result.listing)
                val landmarks = root?.let { shortcutFolder(result.listing, it, InventoryShortcut.LANDMARKS) }
                val current = landmarks ?: root
                copy(
                    state = projectListing(
                        listing = result.listing,
                        currentFolderId = current?.folderId,
                        errorKey = if (landmarks == null) HostessTextKey.BlankStatus else null,
                    ),
                    listing = result.listing,
                )
            }
            is InventoryDirectoryListResult.Failure -> errorState()
        }
    }

    fun openInventoryShortcut(shortcut: InventoryShortcut): InventoryBrowserController {
        val currentListing = listing ?: return errorState()
        val root = rootFolder(currentListing)
        val target = when (shortcut) {
            InventoryShortcut.ROOT -> root
            InventoryShortcut.LANDMARKS,
            InventoryShortcut.TEXTURES,
            -> root?.let { shortcutFolder(currentListing, it, shortcut) }
        }
        return if (target == null) {
            errorState()
        } else {
            copy(projectListing(currentListing, target.folderId), currentListing)
        }
    }

    fun openInventoryFolder(folderId: InventoryFolderId): InventoryBrowserController {
        val currentListing = listing ?: return errorState()
        val target = currentListing.folders.firstOrNull { it.folderId == folderId }
            ?: return errorState()
        return copy(projectListing(currentListing, target.folderId), currentListing)
    }

    fun selectInventoryAsset(itemId: InventoryItemId): InventoryBrowserController {
        val activeSession = session ?: return errorState()
        val currentListing = listing ?: return errorState()
        if (state.visibleAssetRows.none { it.itemId == itemId }) {
            return errorState()
        }
        val descriptor = currentListing.items.firstOrNull { it.itemId == itemId }
            ?: return errorState()
        val currentItems = currentItems(currentListing)
        return when (
            val selected = runtime.inventorySelectionService.selectExistingAttachment(
                items = currentItems,
                displayName = InventoryItemDisplayName(descriptor.displayName.value),
                kind = descriptor.kind,
            )
        ) {
            is InventoryAttachmentSelectionResult.Selected -> {
                when (val resolved = runtime.attachmentService.resolveAttachment(activeSession, selected.request)) {
                    is AttachmentResolutionResult.Resolved -> copy(
                        state.copy(
                            visibleAssetRows = selectAssetRow(itemId),
                            selectedAttachment = SelectedAttachmentUiState(
                                itemId = selected.descriptor.itemId,
                                displayName = selected.descriptor.displayName.value,
                                kind = selected.descriptor.kind,
                                request = selected.request,
                                attachmentRef = resolved.attachment,
                            ),
                            attachmentSummary = HostessTextKey.SelectedCount(1),
                            errorKey = null,
                        ),
                    )
                    is AttachmentResolutionResult.Failed -> selectionFailure()
                }
            }
            is InventoryAttachmentSelectionResult.AmbiguousDisplayName,
            is InventoryAttachmentSelectionResult.NoCopy,
            is InventoryAttachmentSelectionResult.NoSuchItem,
            is InventoryAttachmentSelectionResult.UnknownCopyability,
            is InventoryAttachmentSelectionResult.WrongKind,
            -> selectionFailure()
        }
    }

    private fun projectListing(
        listing: InventoryDirectoryListing,
        currentFolderId: InventoryFolderId?,
        errorKey: HostessTextKey? = null,
    ): InventoryBrowserUiState {
        val currentFolders = currentFolderId?.let { childFolders(listing, it) }.orEmpty()
        val currentItems = currentFolderId?.let { childItems(listing, it) }.orEmpty()
        return state.copy(
            currentFolderId = currentFolderId,
            currentPath = currentPath(listing, currentFolderId),
            shortcuts = shortcutState(listing, currentFolderId),
            visibleFolderRows = currentFolders.map { folder ->
                InventoryFolderRowUiState(
                    folderId = folder.folderId,
                    displayName = folder.displayName.value,
                )
            },
            visibleAssetRows = currentItems.map { item ->
                InventoryAssetRowUiState(
                    itemId = item.itemId,
                    displayName = item.displayName.value,
                    kind = item.kind,
                    selected = item.itemId == state.selectedAttachment?.itemId,
                )
            },
            errorKey = errorKey,
        )
    }

    private fun rootFolder(listing: InventoryDirectoryListing): InventoryFolderDescriptor? =
        listing.folders.firstOrNull { it.parentFolderId == null }

    private fun shortcutFolder(
        listing: InventoryDirectoryListing,
        root: InventoryFolderDescriptor,
        shortcut: InventoryShortcut,
    ): InventoryFolderDescriptor? =
        childFolders(listing, root.folderId)
            .firstOrNull { it.displayName.value.equals(shortcut.name, ignoreCase = true) }

    private fun childFolders(
        listing: InventoryDirectoryListing,
        parentFolderId: InventoryFolderId,
    ): List<InventoryFolderDescriptor> =
        listing.folders.filter { it.parentFolderId == parentFolderId }

    private fun childItems(
        listing: InventoryDirectoryListing,
        parentFolderId: InventoryFolderId,
    ): List<InventoryItemDescriptor> =
        listing.items.filter { it.parentFolderId == parentFolderId }

    private fun currentItems(listing: InventoryDirectoryListing): List<InventoryItemDescriptor> =
        state.currentFolderId?.let { childItems(listing, it) }.orEmpty()

    private fun currentPath(
        listing: InventoryDirectoryListing,
        currentFolderId: InventoryFolderId?,
    ): List<String> {
        val foldersById = listing.folders.associateBy { it.folderId }
        val path = mutableListOf<String>()
        var current = currentFolderId?.let(foldersById::get)
        while (current != null) {
            path += current.displayName.value
            current = current.parentFolderId?.let(foldersById::get)
        }
        return path.asReversed()
    }

    private fun shortcutState(
        listing: InventoryDirectoryListing,
        currentFolderId: InventoryFolderId?,
    ): InventoryShortcutUiState {
        val root = rootFolder(listing)
        val lineage = lineageFolderIds(listing, currentFolderId)
        val landmarks = root?.let { shortcutFolder(listing, it, InventoryShortcut.LANDMARKS) }
        val textures = root?.let { shortcutFolder(listing, it, InventoryShortcut.TEXTURES) }
        return InventoryShortcutUiState(
            rootSelected = root != null && currentFolderId == root.folderId,
            landmarksSelected = landmarks != null && landmarks.folderId in lineage,
            texturesSelected = textures != null && textures.folderId in lineage,
        )
    }

    private fun lineageFolderIds(
        listing: InventoryDirectoryListing,
        currentFolderId: InventoryFolderId?,
    ): Set<InventoryFolderId> {
        val foldersById = listing.folders.associateBy { it.folderId }
        val ids = linkedSetOf<InventoryFolderId>()
        var current = currentFolderId?.let(foldersById::get)
        while (current != null) {
            ids += current.folderId
            current = current.parentFolderId?.let(foldersById::get)
        }
        return ids
    }

    private fun selectAssetRow(itemId: InventoryItemId): List<InventoryAssetRowUiState> =
        state.visibleAssetRows.map { row -> row.copy(selected = row.itemId == itemId) }

    private fun selectionFailure(): InventoryBrowserController =
        copy(
            state.copy(
                visibleAssetRows = state.visibleAssetRows.map { it.copy(selected = false) },
                selectedAttachment = null,
                attachmentSummary = HostessTextKey.None,
                errorKey = HostessTextKey.BlankStatus,
            ),
        )

    private fun errorState(): InventoryBrowserController =
        copy(
            state.copy(
                errorKey = HostessTextKey.BlankStatus,
            ),
        )

    private fun copy(
        state: InventoryBrowserUiState,
        listing: InventoryDirectoryListing? = this.listing,
    ): InventoryBrowserController =
        InventoryBrowserController(runtime, session, state, listing)
}
