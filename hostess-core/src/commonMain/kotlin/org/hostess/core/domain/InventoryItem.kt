package org.hostess.core.domain

enum class InventoryItemKind {
    LANDMARK,
    TEXTURE,
    NOTECARD,
}

data class InventoryItemDescriptor(
    val itemId: InventoryItemId,
    val parentFolderId: InventoryFolderId,
    val assetId: InventoryAssetId,
    val displayName: InventoryItemDisplayName,
    val kind: InventoryItemKind,
    val copyable: Boolean? = null,
    val ownerId: AttachmentOwnerId? = null,
)

data class InventoryItemQuery(
    val kinds: Set<InventoryItemKind> = setOf(InventoryItemKind.LANDMARK),
    val displayNameContains: String? = null,
) {
    init {
        require(kinds.isNotEmpty()) { "InventoryItemQuery kinds cannot be empty." }
    }
}
