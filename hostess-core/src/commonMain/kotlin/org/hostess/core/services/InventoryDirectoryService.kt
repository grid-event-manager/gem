package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.InventoryDirectoryListResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.ports.InventoryPort

class InventoryDirectoryService(
    private val inventoryPort: InventoryPort,
) {
    fun listItems(
        session: HostessSession,
        query: InventoryItemQuery = InventoryItemQuery(),
    ): InventoryItemListResult = inventoryPort.listItems(session, query)

    fun listDirectory(
        session: HostessSession,
        query: InventoryItemQuery = InventoryItemQuery(),
    ): InventoryDirectoryListResult = inventoryPort.listDirectory(session, query)
}
