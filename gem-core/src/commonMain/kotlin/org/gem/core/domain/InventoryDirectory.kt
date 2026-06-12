package org.gem.core.domain

@JvmInline
value class InventoryFolderDisplayName(val value: String) {
    init {
        require(value.isNotBlank()) { "InventoryFolderDisplayName cannot be blank." }
    }
}

data class InventoryFolderDescriptor(
    val folderId: InventoryFolderId,
    val parentFolderId: InventoryFolderId?,
    val displayName: InventoryFolderDisplayName,
)

class InventoryDirectoryListing(
    folders: List<InventoryFolderDescriptor>,
    items: List<InventoryItemDescriptor>,
) {
    val folders: List<InventoryFolderDescriptor> = folders.toList()
    val items: List<InventoryItemDescriptor> = items.toList()

    override fun equals(other: Any?): Boolean =
        other is InventoryDirectoryListing &&
            folders == other.folders &&
            items == other.items

    override fun hashCode(): Int = 31 * folders.hashCode() + items.hashCode()

    override fun toString(): String =
        "InventoryDirectoryListing(folders=$folders, items=$items)"
}
