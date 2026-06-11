package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.mapping.LibomvInventoryFolderSnapshot
import org.hostess.protocol.libomv.mapping.LibomvInventoryItemSnapshot

internal data class InventorySnapshot(
    val folders: List<LibomvInventoryFolderSnapshot>,
    val items: List<LibomvInventoryItemSnapshot>,
)

internal data class InventorySnapshotCacheKey(
    val agentId: String,
    val rootId: String,
)

internal interface InventorySnapshotCacheStore {
    fun load(key: InventorySnapshotCacheKey): InventorySnapshot?

    fun save(
        key: InventorySnapshotCacheKey,
        snapshot: InventorySnapshot,
    )

    companion object {
        fun unavailable(): InventorySnapshotCacheStore = NoopInventorySnapshotCacheStore
    }
}

private object NoopInventorySnapshotCacheStore : InventorySnapshotCacheStore {
    override fun load(key: InventorySnapshotCacheKey): InventorySnapshot? = null

    override fun save(
        key: InventorySnapshotCacheKey,
        snapshot: InventorySnapshot,
    ) = Unit
}
