package org.gem.core.services

import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.ports.InventoryPort

class InventoryDirectoryService(
    private val inventoryPort: InventoryPort,
) {
    fun listItems(
        session: GemSession,
        query: InventoryItemQuery = InventoryItemQuery(),
    ): InventoryItemListResult = inventoryPort.listItems(session, query)

    fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery = InventoryItemQuery(),
    ): InventoryDirectoryListResult = inventoryPort.listDirectory(session, query)
}
