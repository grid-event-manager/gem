package org.gem.ui.testing

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.AttachmentOwnerId
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryAssetId
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryFolderDescriptor
import org.gem.core.domain.InventoryFolderDisplayName
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemDisplayName
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemKind
import org.gem.core.domain.SessionId

object FakeInventoryFixtures {
    val rootFolderId = InventoryFolderId("folder:root")
    val landmarksFolderId = InventoryFolderId("folder:landmarks")
    val venuesFolderId = InventoryFolderId("folder:venues")
    val texturesFolderId = InventoryFolderId("folder:textures")
    val welcomeItemId = InventoryItemId("item:welcome")
    val venueItemId = InventoryItemId("item:venue")
    val textureItemId = InventoryItemId("item:texture")
    val noCopyItemId = InventoryItemId("item:no-copy")

    fun session(): GemSession =
        GemSession(
            sessionId = SessionId("session:ui"),
            accountLabel = AccountLabel("venuehost resident"),
            startedAt = GemInstant.EPOCH,
            isActive = true,
        )

    fun listing(): InventoryDirectoryListing =
        InventoryDirectoryListing(
            folders = listOf(
                folder(rootFolderId, null, "Inventory"),
                folder(landmarksFolderId, rootFolderId, "Landmarks"),
                folder(venuesFolderId, landmarksFolderId, "Venues"),
                folder(texturesFolderId, rootFolderId, "Textures"),
            ),
            items = listOf(
                item(welcomeItemId, landmarksFolderId, "Welcome Area", InventoryItemKind.LANDMARK),
                item(venueItemId, venuesFolderId, "Venue Stage", InventoryItemKind.LANDMARK),
                item(textureItemId, texturesFolderId, "Stage Poster", InventoryItemKind.TEXTURE),
                item(noCopyItemId, landmarksFolderId, "No Copy Landmark", InventoryItemKind.LANDMARK, copyable = false),
            ),
        )

    fun listingWithoutAttachmentOwners(): InventoryDirectoryListing =
        InventoryDirectoryListing(
            folders = listOf(
                folder(rootFolderId, null, "Inventory"),
                folder(landmarksFolderId, rootFolderId, "Landmarks"),
                folder(texturesFolderId, rootFolderId, "Textures"),
            ),
            items = listOf(
                item(welcomeItemId, landmarksFolderId, "Welcome Area", InventoryItemKind.LANDMARK, ownerId = null),
            ),
        )

    fun listingWithoutLandmarks(): InventoryDirectoryListing =
        InventoryDirectoryListing(
            folders = listOf(
                folder(rootFolderId, null, "Inventory"),
                folder(texturesFolderId, rootFolderId, "Textures"),
            ),
            items = emptyList(),
        )

    fun listingWithEmptyLandmarks(): InventoryDirectoryListing =
        InventoryDirectoryListing(
            folders = listOf(
                folder(rootFolderId, null, "Inventory"),
                folder(landmarksFolderId, rootFolderId, "Landmarks"),
            ),
            items = emptyList(),
        )

    private fun folder(
        id: InventoryFolderId,
        parentId: InventoryFolderId?,
        displayName: String,
    ): InventoryFolderDescriptor =
        InventoryFolderDescriptor(id, parentId, InventoryFolderDisplayName(displayName))

    private fun item(
        id: InventoryItemId,
        parentId: InventoryFolderId,
        displayName: String,
        kind: InventoryItemKind,
        copyable: Boolean? = true,
        ownerId: AttachmentOwnerId? = AttachmentOwnerId("owner:${id.value}"),
    ): InventoryItemDescriptor =
        InventoryItemDescriptor(
            itemId = id,
            parentFolderId = parentId,
            assetId = InventoryAssetId("asset:${id.value}"),
            displayName = InventoryItemDisplayName(displayName),
            kind = kind,
            copyable = copyable,
            ownerId = ownerId,
        )
}
