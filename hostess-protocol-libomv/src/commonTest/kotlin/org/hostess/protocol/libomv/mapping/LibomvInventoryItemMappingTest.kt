package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.InventoryItemKind
import org.hostess.protocol.libomv.llsd.LlsdValue
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
        assertNull(landmark.copyable)
        assertEquals("notecard", notecard.itemId.value)
        assertEquals(InventoryItemKind.NOTECARD, notecard.kind)
        assertNull(notecard.copyable)
    }

    @Test
    fun `maps owner copy permission bit into descriptor copyability`() {
        val copyable = LibomvInventoryItemMapping.descriptor(
            snapshot("copyable", "Copyable Landmark", 3, permissions(1L shl 15)),
        )
        val noCopy = LibomvInventoryItemMapping.descriptor(
            snapshot("no-copy", "No Copy Landmark", 3, permissions(0)),
        )

        requireNotNull(copyable)
        requireNotNull(noCopy)
        assertEquals(true, copyable.copyable)
        assertEquals(false, noCopy.copyable)
    }

    @Test
    fun `malformed owner mask maps descriptor copyability to null`() {
        val missingPermissions = LibomvInventoryItemMapping.descriptor(
            snapshot("missing", "Missing Permissions", 3),
        )
        val missingOwnerMask = LibomvInventoryItemMapping.descriptor(
            snapshot("missing-mask", "Missing Owner Mask", 3, LlsdValue.MapValue(emptyMap())),
        )
        val malformedOwnerMask = LibomvInventoryItemMapping.descriptor(
            snapshot("malformed", "Malformed Permissions", 3, permissions(LlsdValue.ScalarValue("not-a-number"))),
        )
        val negativeOwnerMask = LibomvInventoryItemMapping.descriptor(
            snapshot("negative", "Negative Permissions", 3, permissions(-1)),
        )
        val nonMapPermissions = LibomvInventoryItemMapping.descriptor(
            snapshot("non-map", "Non Map Permissions", 3, LlsdValue.ScalarValue("32768")),
        )

        requireNotNull(missingPermissions)
        requireNotNull(missingOwnerMask)
        requireNotNull(malformedOwnerMask)
        requireNotNull(negativeOwnerMask)
        requireNotNull(nonMapPermissions)
        assertNull(missingPermissions.copyable)
        assertNull(missingOwnerMask.copyable)
        assertNull(malformedOwnerMask.copyable)
        assertNull(negativeOwnerMask.copyable)
        assertNull(nonMapPermissions.copyable)
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
        permissions: LlsdValue? = null,
    ): LibomvInventoryItemSnapshot = LibomvInventoryItemSnapshot(
        itemId = itemId,
        ownerId = "11111111-1111-1111-1111-111111111111",
        parentFolderId = "22222222-2222-2222-2222-222222222222",
        assetId = "33333333-3333-3333-3333-333333333333",
        name = name,
        inventoryType = inventoryType,
        permissions = permissions,
    )

    private fun permissions(ownerMask: Long): LlsdValue =
        permissions(LlsdValue.ScalarValue(ownerMask.toString()))

    private fun permissions(ownerMask: LlsdValue): LlsdValue =
        LlsdValue.MapValue(mapOf("owner_mask" to ownerMask))
}
