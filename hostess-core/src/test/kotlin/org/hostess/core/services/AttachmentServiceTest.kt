package org.hostess.core.services

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftInvalidReason
import org.hostess.core.domain.NoticeDraftValidation
import org.hostess.core.domain.TargetSelectionResult
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.testing.FakeInventoryPort
import org.hostess.core.testing.defaultSession
import org.hostess.core.testing.failure
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
            textureResult = AttachmentResolutionResult.Failed(
                failure(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, "upload rejected"),
            ),
        )
        val service = AttachmentService(inventoryPort)

        val result = assertIs<AttachmentResolutionResult.Failed>(
            service.resolveAttachment(
                defaultSession(),
                UploadTextureAttachment("poster.png", "sha256:abc"),
            ),
        )

        assertEquals(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, result.failure.reason)
        assertEquals("upload rejected", result.failure.redactedMessage)
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
                UploadTextureAttachment("poster.png", "sha256:abc"),
            ),
        )

        val validation = assertIs<NoticeDraftValidation.Invalid>(draft.validateForSend())

        assertTrue(NoticeDraftInvalidReason.TOO_MANY_ATTACHMENTS in validation.reasons)
        assertTrue(inventoryPort.existingRequests.isEmpty())
        assertTrue(inventoryPort.textureRequests.isEmpty())
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
