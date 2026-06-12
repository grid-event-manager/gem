package org.hostess.protocol.libomv.runtime

import java.nio.file.Files
import java.nio.file.Path
import org.hostess.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileInventorySnapshotCacheStoreTest {
    @Test
    fun `persists snapshot across store instances`() {
        withTempDirectory { dir ->
            val key = InventorySnapshotCacheKey("agent-id", "root-id")
            val snapshot = InventorySnapshot(
                folders = listOf(LibomvInventoryFolderSnapshot("root-id", null, "Inventory")),
                items = listOf(
                    LibomvInventoryItemSnapshot(
                        itemId = "landmark-item",
                        ownerId = "agent-id",
                        parentFolderId = "root-id",
                        assetId = "asset-id",
                        name = "Welcome Area",
                        inventoryType = 3,
                        copyable = true,
                    ),
                ),
            )

            FileInventorySnapshotCacheStore(dir).save(key, snapshot)

            assertEquals(snapshot, FileInventorySnapshotCacheStore(dir).load(key))
        }
    }

    @Test
    fun `treats corrupt cache file as miss`() {
        withTempDirectory { dir ->
            Files.createDirectories(dir)
            Files.write(dir.resolve("inventory-agent-id-root-id.tsv"), "bad".encodeToByteArray())

            assertNull(FileInventorySnapshotCacheStore(dir).load(InventorySnapshotCacheKey("agent-id", "root-id")))
        }
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("hostess-inventory-cache-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
