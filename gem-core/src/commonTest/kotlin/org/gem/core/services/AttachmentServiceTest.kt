package org.gem.core.services

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GroupDisplayName
import org.gem.core.domain.GroupId
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeDraftInvalidReason
import org.gem.core.domain.NoticeDraftValidation
import org.gem.core.domain.TargetSelectionResult
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.testing.FakeInventoryPort
import org.gem.core.testing.defaultSession
import org.gem.core.testing.failure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AttachmentServiceTest {
    @Test
    fun `resolves existing inventory attachment through inventory port`() {
        val inventoryPort = FakeInventoryPort()
        val service = AttachmentService(inventoryPort)
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark"))

        assertIs<AttachmentResolutionResult.Resolved>(
            service.resolveAttachment(defaultSession(), request),
        )
        assertEquals(listOf(request), inventoryPort.existingRequests)
    }

    @Test
    fun `preserves adapter failure mapping from inventory port`() {
        val inventoryPort = FakeInventoryPort(
            existingResult = AttachmentResolutionResult.Failed(
                failure(CoreFailureReason.ATTACHMENT_NOT_FOUND, "attachment rejected"),
            ),
        )
        val service = AttachmentService(inventoryPort)
        val request = ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("texture-item"))

        val result = assertIs<AttachmentResolutionResult.Failed>(
            service.resolveAttachment(
                defaultSession(),
                request,
            ),
        )

        assertEquals(listOf(request), inventoryPort.existingRequests)
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, result.failure.reason)
        assertEquals("attachment rejected", result.failure.redactedMessage)
    }

    @Test
    fun `draft with multiple attachments is invalid before adapter resolution`() {
        val inventoryPort = FakeInventoryPort()
        val draft = NoticeDraft(
            subject = "Opening set",
            message = "Tonight at 8",
            targetSet = selectedTargets(),
            attachments = listOf(
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark")),
                ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("texture")),
            ),
        )

        val validation = assertIs<NoticeDraftValidation.Invalid>(draft.validateForSend())

        assertTrue(NoticeDraftInvalidReason.TOO_MANY_ATTACHMENTS in validation.reasons)
        assertTrue(inventoryPort.existingRequests.isEmpty())
    }

    private fun selectedTargets(): GroupTargetSet = assertIs<TargetSelectionResult.Changed>(
        GroupTargetSet.from(listOf(group("music", "Music Room"))).addAllSendable(),
    ).targetSet

    private fun group(id: String, displayName: String): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = true,
        acceptsNotices = null,
    )
}
