package org.hostess.core.services

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.core.testing.FakeInventoryPort
import org.hostess.core.testing.defaultInventoryItem
import org.hostess.core.testing.defaultSession
import org.hostess.core.testing.failure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InventoryDirectoryServiceTest {
    @Test
    fun `delegates item listing to inventory port`() {
        val item = defaultInventoryItem()
        val inventoryPort = FakeInventoryPort(
            listResult = InventoryItemListResult.Success(listOf(item)),
        )
        val service = InventoryDirectoryService(inventoryPort)
        val query = InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK))

        val result = assertIs<InventoryItemListResult.Success>(
            service.listItems(defaultSession(), query),
        )

        assertEquals(listOf(item), result.items)
        assertEquals(listOf(query), inventoryPort.listRequests)
    }

    @Test
    fun `preserves inventory list failure from port`() {
        val inventoryPort = FakeInventoryPort(
            listResult = InventoryItemListResult.Failure(
                failure(CoreFailureReason.INVENTORY_LIST_FAILED, "inventory unavailable"),
            ),
        )
        val service = InventoryDirectoryService(inventoryPort)

        val result = assertIs<InventoryItemListResult.Failure>(
            service.listItems(defaultSession(), InventoryItemQuery()),
        )

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, result.failure.reason)
        assertEquals("inventory unavailable", result.failure.redactedMessage)
    }
}
