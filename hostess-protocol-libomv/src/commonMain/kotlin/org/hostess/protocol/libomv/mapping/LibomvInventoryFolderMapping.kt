package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.InventoryFolderDescriptor
import org.hostess.core.domain.InventoryFolderDisplayName
import org.hostess.core.domain.InventoryFolderId

internal data class LibomvInventoryFolderSnapshot(
    val folderId: String,
    val parentFolderId: String?,
    val name: String,
)

internal object LibomvInventoryFolderMapping {
    fun descriptor(snapshot: LibomvInventoryFolderSnapshot): InventoryFolderDescriptor? {
        if (snapshot.folderId.isBlank() || snapshot.name.isBlank()) {
            return null
        }
        return InventoryFolderDescriptor(
            folderId = InventoryFolderId(snapshot.folderId),
            parentFolderId = snapshot.parentFolderId
                ?.takeIf(String::isNotBlank)
                ?.let(::InventoryFolderId),
            displayName = InventoryFolderDisplayName(snapshot.name),
        )
    }
}
