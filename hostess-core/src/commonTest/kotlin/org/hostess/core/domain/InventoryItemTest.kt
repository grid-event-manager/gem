package org.hostess.core.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

class InventoryItemTest {
    @Test
    fun `inventory identifiers reject blank values`() {
        assertFailsWith<IllegalArgumentException> { InventoryItemDisplayName(" ") }
        assertFailsWith<IllegalArgumentException> { InventoryFolderId(" ") }
        assertFailsWith<IllegalArgumentException> { InventoryAssetId(" ") }
    }

    @Test
    fun `inventory item query defaults to landmarks and rejects empty kinds`() {
        InventoryItemQuery()

        assertFailsWith<IllegalArgumentException> {
            InventoryItemQuery(kinds = emptySet())
        }
    }
}
