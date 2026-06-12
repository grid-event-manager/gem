package org.gem.protocol.libomv.runtime

import org.gem.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.gem.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InventorySnapshotCacheCodecTest {
    @Test
    fun `round trips snapshot metadata used by attachment selection`() {
        val snapshot = InventorySnapshot(
            folders = listOf(
                LibomvInventoryFolderSnapshot(
                    folderId = "root",
                    parentFolderId = null,
                    name = "Inventory",
                ),
                LibomvInventoryFolderSnapshot(
                    folderId = "landmarks",
                    parentFolderId = "root",
                    name = "Landmarks\tVenue",
                ),
            ),
            items = listOf(
                LibomvInventoryItemSnapshot(
                    itemId = "landmark-item",
                    ownerId = "owner-id",
                    parentFolderId = "landmarks",
                    assetId = "asset-id",
                    name = "Welcome % Area",
                    inventoryType = 3,
                    copyable = true,
                ),
                LibomvInventoryItemSnapshot(
                    itemId = "texture-item",
                    ownerId = "owner-id",
                    parentFolderId = "textures",
                    assetId = "texture-asset",
                    name = "Poster",
                    inventoryType = 0,
                    copyable = null,
                ),
            ),
        )

        val decoded = assertIs<InventorySnapshot>(
            InventorySnapshotCacheCodec.decode(InventorySnapshotCacheCodec.encode(snapshot)),
        )

        assertEquals(snapshot.folders, decoded.folders)
        assertEquals(snapshot.items, decoded.items)
    }

    @Test
    fun `rejects corrupt payload`() {
        assertNull(InventorySnapshotCacheCodec.decode("not a cache"))
        assertNull(InventorySnapshotCacheCodec.decode("HOSTESS_INVENTORY_SNAPSHOT_CACHE_V1\nitem\tbad"))
    }
}
