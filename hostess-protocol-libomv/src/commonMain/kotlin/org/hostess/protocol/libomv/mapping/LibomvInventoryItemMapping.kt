package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.InventoryAssetId
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemDisplayName
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.protocol.libomv.llsd.LlsdValue

internal data class LibomvInventoryItemSnapshot(
    val itemId: String,
    val ownerId: String,
    val parentFolderId: String,
    val assetId: String,
    val name: String,
    val inventoryType: Int,
    val permissions: LlsdValue? = null,
)

internal object LibomvInventoryItemMapping {
    fun descriptor(snapshot: LibomvInventoryItemSnapshot): InventoryItemDescriptor? {
        if (
            snapshot.itemId.isBlank() ||
            snapshot.ownerId.isBlank() ||
            snapshot.parentFolderId.isBlank() ||
            snapshot.assetId.isBlank() ||
            snapshot.name.isBlank()
        ) {
            return null
        }
        val kind = when (snapshot.inventoryType) {
            LANDMARK_INVENTORY_TYPE -> InventoryItemKind.LANDMARK
            NOTECARD_INVENTORY_TYPE -> InventoryItemKind.NOTECARD
            else -> return null
        }
        return InventoryItemDescriptor(
            itemId = InventoryItemId(snapshot.itemId),
            parentFolderId = InventoryFolderId(snapshot.parentFolderId),
            assetId = InventoryAssetId(snapshot.assetId),
            displayName = InventoryItemDisplayName(snapshot.name),
            kind = kind,
            copyable = null,
        )
    }

    private const val LANDMARK_INVENTORY_TYPE = 3
    private const val NOTECARD_INVENTORY_TYPE = 7
}
