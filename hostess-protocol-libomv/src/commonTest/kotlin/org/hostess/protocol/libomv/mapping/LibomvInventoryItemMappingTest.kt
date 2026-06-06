package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.InventoryItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibomvInventoryItemMappingTest {
    @Test
    fun `maps landmark and notecard descriptors`() {
        val landmark = LibomvInventoryItemMapping.descriptor(snapshot("landmark", "Venue Landmark", 3))
        val notecard = LibomvInventoryItemMapping.descriptor(snapshot("notecard", "Venue Notes", 7))

        requireNotNull(landmark)
        requireNotNull(notecard)
        assertEquals("landmark", landmark.itemId.value)
        assertEquals("Venue Landmark", landmark.displayName.value)
        assertEquals(InventoryItemKind.LANDMARK, landmark.kind)
        assertEquals("notecard", notecard.itemId.value)
        assertEquals(InventoryItemKind.NOTECARD, notecard.kind)
    }

    @Test
    fun `unknown inventory type and malformed item return null`() {
        assertNull(LibomvInventoryItemMapping.descriptor(snapshot("object", "Object", 6)))
        assertNull(LibomvInventoryItemMapping.descriptor(snapshot("", "Venue Landmark", 3)))
        assertNull(LibomvInventoryItemMapping.descriptor(snapshot("landmark", "", 3)))
    }

    private fun snapshot(
        itemId: String,
        name: String,
        inventoryType: Int,
    ): LibomvInventoryItemSnapshot = LibomvInventoryItemSnapshot(
        itemId = itemId,
        ownerId = "11111111-1111-1111-1111-111111111111",
        parentFolderId = "22222222-2222-2222-2222-222222222222",
        assetId = "33333333-3333-3333-3333-333333333333",
        name = name,
        inventoryType = inventoryType,
    )
}
