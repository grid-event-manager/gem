package org.gem.ui.controllers

import org.gem.core.domain.GemSession
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.InventoryAttachmentSelectionResult
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryFolderDescriptor
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemDisplayName
import org.gem.core.domain.InventoryItemKind
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.state.InventoryAssetRowUiState
import org.gem.ui.state.InventoryBrowserUiState
import org.gem.ui.state.InventoryFolderRowUiState
import org.gem.ui.state.InventoryShortcut
import org.gem.ui.state.InventoryShortcutUiState
import org.gem.ui.state.SelectedAttachmentUiState
import org.gem.ui.text.GemTextKey

class InventoryBrowserController(
    val runtime: GemUiRuntime,
    val session: GemSession? = null,
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
                        errorKey = if (landmarks == null) GemTextKey.InventoryUnavailable else null,
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
        return when (
            val selected = runtime.inventorySelectionService.selectExistingAttachment(
                items = listOf(descriptor),
                displayName = InventoryItemDisplayName(descriptor.displayName.value),
                kind = descriptor.kind,
            )
        ) {
            is InventoryAttachmentSelectionResult.Selected -> {
                selected.attachmentRef?.let { attachment ->
                    return resolvedSelection(selected, attachment)
                }
                when (val resolved = runtime.attachmentService.resolveAttachment(activeSession, selected.request)) {
                    is AttachmentResolutionResult.Resolved -> resolvedSelection(selected, resolved.attachment)
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

    fun clearSelectedAttachment(): InventoryBrowserController =
        copy(
            state.copy(
                visibleAssetRows = state.visibleAssetRows.map { row -> row.copy(selected = false) },
                selectedAttachment = null,
                attachmentSummary = GemTextKey.None,
                errorKey = null,
            ),
        )

    private fun projectListing(
        listing: InventoryDirectoryListing,
        currentFolderId: InventoryFolderId?,
        errorKey: GemTextKey? = null,
    ): InventoryBrowserUiState {
        val currentFolders = currentFolderId?.let { childFolders(listing, it) }.orEmpty()
        val currentItems = currentFolderId?.let { childItems(listing, it) }.orEmpty()
        return state.copy(
            loading = false,
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

    private fun resolvedSelection(
        selected: InventoryAttachmentSelectionResult.Selected,
        attachment: AttachmentRef,
    ): InventoryBrowserController =
        copy(
            state.copy(
                visibleAssetRows = selectAssetRow(selected.descriptor.itemId),
                selectedAttachment = SelectedAttachmentUiState(
                    itemId = selected.descriptor.itemId,
                    displayName = selected.descriptor.displayName.value,
                    kind = selected.descriptor.kind,
                    request = selected.request,
                    attachmentRef = attachment,
                ),
                attachmentSummary = GemTextKey.SelectedCount(1),
                errorKey = null,
            ),
        )

    private fun selectionFailure(): InventoryBrowserController =
        copy(
            state.copy(
                visibleAssetRows = state.visibleAssetRows.map { it.copy(selected = false) },
                selectedAttachment = null,
                attachmentSummary = GemTextKey.None,
                errorKey = GemTextKey.InventoryUnavailable,
            ),
        )

    private fun errorState(): InventoryBrowserController =
        copy(
            state.copy(
                loading = false,
                errorKey = GemTextKey.InventoryUnavailable,
            ),
        )

    private fun copy(
        state: InventoryBrowserUiState,
        listing: InventoryDirectoryListing? = this.listing,
    ): InventoryBrowserController =
        InventoryBrowserController(runtime, session, state, listing)
}
