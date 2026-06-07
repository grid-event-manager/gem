package org.hostess.core.services

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.InventoryAssetId
import org.hostess.core.domain.InventoryAttachmentSelectionResult
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemDisplayName
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InventorySelectionServiceTest {
    private val service = InventorySelectionService()

    @Test
    fun `selects one exact display name copyable landmark`() {
        val result = assertIs<InventoryAttachmentSelectionResult.Selected>(
            service.selectExistingAttachment(
                listOf(item("landmark", "Venue Landmark", copyable = true)),
                InventoryItemDisplayName("Venue Landmark"),
            ),
        )

        assertEquals("landmark", result.request.itemId.value)
        assertEquals(AttachmentKind.LANDMARK, result.request.kind)
        assertEquals("Venue Landmark", result.descriptor.displayName.value)
    }

    @Test
    fun `display name helper keeps string conversion in core`() {
        val result = assertIs<InventoryAttachmentSelectionResult.Selected>(
            service.selectExistingAttachmentByDisplayName(
                listOf(item("landmark", "Venue Landmark", copyable = true)),
                "Venue Landmark",
            ),
        )

        assertEquals("landmark", result.request.itemId.value)
    }

    @Test
    fun `missing display name returns no such item`() {
        val result = service.selectExistingAttachment(
            listOf(item("landmark", "Venue Landmark", copyable = true)),
            InventoryItemDisplayName("Other Landmark"),
        )

        assertIs<InventoryAttachmentSelectionResult.NoSuchItem>(result)
    }

    @Test
    fun `exact display name is case-sensitive`() {
        val result = service.selectExistingAttachment(
            listOf(item("landmark", "Venue Landmark", copyable = true)),
            InventoryItemDisplayName("venue landmark"),
        )

        assertIs<InventoryAttachmentSelectionResult.NoSuchItem>(result)
    }

    @Test
    fun `wrong kind returns all exact display name matches`() {
        val matches = listOf(item("note", "Venue Item", kind = InventoryItemKind.NOTECARD, copyable = true))

        val result = assertIs<InventoryAttachmentSelectionResult.WrongKind>(
            service.selectExistingAttachment(matches, InventoryItemDisplayName("Venue Item")),
        )

        assertEquals(InventoryItemKind.LANDMARK, result.requiredKind)
        assertEquals(matches, result.matches)
    }

    @Test
    fun `non-landmark requested kind cannot produce a landmark attachment request`() {
        val matches = listOf(item("note", "Venue Item", kind = InventoryItemKind.NOTECARD, copyable = true))

        val result = assertIs<InventoryAttachmentSelectionResult.WrongKind>(
            service.selectExistingAttachment(
                matches,
                InventoryItemDisplayName("Venue Item"),
                kind = InventoryItemKind.NOTECARD,
            ),
        )

        assertEquals(InventoryItemKind.LANDMARK, result.requiredKind)
    }

    @Test
    fun `duplicate requested kind returns ambiguous display name`() {
        val first = item("first", "Venue Landmark", copyable = true)
        val second = item("second", "Venue Landmark", copyable = true)

        val result = assertIs<InventoryAttachmentSelectionResult.AmbiguousDisplayName>(
            service.selectExistingAttachment(listOf(first, second), InventoryItemDisplayName("Venue Landmark")),
        )

        assertEquals(listOf(first, second), result.matches)
    }

    @Test
    fun `wrong-kind duplicate plus one valid landmark selects the valid landmark`() {
        val notecard = item("note", "Venue Item", kind = InventoryItemKind.NOTECARD, copyable = true)
        val landmark = item("landmark", "Venue Item", kind = InventoryItemKind.LANDMARK, copyable = true)

        val result = assertIs<InventoryAttachmentSelectionResult.Selected>(
            service.selectExistingAttachment(listOf(notecard, landmark), InventoryItemDisplayName("Venue Item")),
        )

        assertEquals("landmark", result.request.itemId.value)
    }

    @Test
    fun `no-copy and unknown-copy landmarks block live attachment selection`() {
        assertIs<InventoryAttachmentSelectionResult.NoCopy>(
            service.selectExistingAttachment(
                listOf(item("no-copy", "Venue Landmark", copyable = false)),
                InventoryItemDisplayName("Venue Landmark"),
            ),
        )
        assertIs<InventoryAttachmentSelectionResult.UnknownCopyability>(
            service.selectExistingAttachment(
                listOf(item("unknown", "Venue Landmark", copyable = null)),
                InventoryItemDisplayName("Venue Landmark"),
            ),
        )
    }

    @Test
    fun `copyability requirement can be explicitly relaxed`() {
        val result = assertIs<InventoryAttachmentSelectionResult.Selected>(
            service.selectExistingAttachment(
                listOf(item("unknown", "Venue Landmark", copyable = null)),
                InventoryItemDisplayName("Venue Landmark"),
                requireCopyable = false,
            ),
        )

        assertEquals("unknown", result.request.itemId.value)
    }

    private fun item(
        id: String,
        displayName: String,
        kind: InventoryItemKind = InventoryItemKind.LANDMARK,
        copyable: Boolean?,
    ): InventoryItemDescriptor = InventoryItemDescriptor(
        itemId = InventoryItemId(id),
        parentFolderId = InventoryFolderId("folder-$id"),
        assetId = InventoryAssetId("asset-$id"),
        displayName = InventoryItemDisplayName(displayName),
        kind = kind,
        copyable = copyable,
    )
}
