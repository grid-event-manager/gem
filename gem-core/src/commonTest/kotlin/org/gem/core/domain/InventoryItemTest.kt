package org.gem.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InventoryItemTest {
    @Test
    fun `inventory identifiers reject blank values`() {
        assertFailsWith<IllegalArgumentException> { InventoryItemDisplayName(" ") }
        assertFailsWith<IllegalArgumentException> { InventoryFolderId(" ") }
        assertFailsWith<IllegalArgumentException> { InventoryFolderDisplayName(" ") }
        assertFailsWith<IllegalArgumentException> { InventoryAssetId(" ") }
    }

    @Test
    fun `inventory item query defaults to landmarks and rejects empty kinds`() {
        InventoryItemQuery()

        assertFailsWith<IllegalArgumentException> {
            InventoryItemQuery(kinds = emptySet())
        }
    }

    @Test
    fun `inventory directory listing defensively copies folders and items`() {
        val folders = mutableListOf(
            InventoryFolderDescriptor(
                folderId = InventoryFolderId("folder"),
                parentFolderId = null,
                displayName = InventoryFolderDisplayName("Landmarks"),
            ),
        )
        val items = mutableListOf(
            InventoryItemDescriptor(
                itemId = InventoryItemId("landmark"),
                parentFolderId = InventoryFolderId("folder"),
                assetId = InventoryAssetId("asset"),
                displayName = InventoryItemDisplayName("Venue Landmark"),
                kind = InventoryItemKind.LANDMARK,
            ),
        )

        val listing = InventoryDirectoryListing(folders, items)
        folders.clear()
        items.clear()

        assertEquals(listOf("Landmarks"), listing.folders.map { it.displayName.value })
        assertEquals(listOf("Venue Landmark"), listing.items.map { it.displayName.value })
    }
}
