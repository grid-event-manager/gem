package org.gem.core.services

import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryFolderDescriptor
import org.gem.core.domain.InventoryFolderDisplayName
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemKind
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.testing.FakeInventoryPort
import org.gem.core.testing.defaultInventoryItem
import org.gem.core.testing.defaultSession
import org.gem.core.testing.failure
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
    fun `delegates directory listing to inventory port`() {
        val item = defaultInventoryItem()
        val folder = InventoryFolderDescriptor(
            folderId = InventoryFolderId("landmarks"),
            parentFolderId = null,
            displayName = InventoryFolderDisplayName("Landmarks"),
        )
        val inventoryPort = FakeInventoryPort(
            directoryResult = InventoryDirectoryListResult.Success(
                InventoryDirectoryListing(listOf(folder), listOf(item)),
            ),
        )
        val service = InventoryDirectoryService(inventoryPort)
        val query = InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK))

        val result = assertIs<InventoryDirectoryListResult.Success>(
            service.listDirectory(defaultSession(), query),
        )

        assertEquals(listOf(folder), result.listing.folders)
        assertEquals(listOf(item), result.listing.items)
        assertEquals(listOf(query), inventoryPort.directoryRequests)
    }

    @Test
    fun `item listing projects from directory listing`() {
        val item = defaultInventoryItem()
        val inventoryPort = FakeInventoryPort(
            directoryResult = InventoryDirectoryListResult.Success(
                InventoryDirectoryListing(
                    folders = listOf(
                        InventoryFolderDescriptor(
                            folderId = InventoryFolderId("landmarks"),
                            parentFolderId = null,
                            displayName = InventoryFolderDisplayName("Landmarks"),
                        ),
                    ),
                    items = listOf(item),
                ),
            ),
        )
        val service = InventoryDirectoryService(inventoryPort)
        val query = InventoryItemQuery(kinds = setOf(InventoryItemKind.LANDMARK))

        val result = assertIs<InventoryItemListResult.Success>(
            service.listItems(defaultSession(), query),
        )

        assertEquals(listOf(item), result.items)
        assertEquals(listOf(query), inventoryPort.directoryRequests)
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

    @Test
    fun `preserves inventory directory failure from port`() {
        val inventoryPort = FakeInventoryPort(
            directoryResult = InventoryDirectoryListResult.Failure(
                failure(CoreFailureReason.INVENTORY_LIST_FAILED, "inventory unavailable"),
            ),
        )
        val service = InventoryDirectoryService(inventoryPort)

        val result = assertIs<InventoryDirectoryListResult.Failure>(
            service.listDirectory(defaultSession(), InventoryItemQuery()),
        )

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, result.failure.reason)
        assertEquals("inventory unavailable", result.failure.redactedMessage)
    }
}
