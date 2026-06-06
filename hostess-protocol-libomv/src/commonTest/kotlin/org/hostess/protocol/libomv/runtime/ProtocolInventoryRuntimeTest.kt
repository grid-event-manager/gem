package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ProtocolInventoryRuntimeTest {
    @Test
    fun `existing item resolves when kind matches`() {
        val session = hostessSession()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK))
        }

        val result = runtime(session, source).resolveExistingAttachment(session, request)

        val attachment = assertIs<AttachmentResolutionResult.Resolved>(result).attachment
        assertEquals("landmark-item", attachment.attachmentId.value)
        assertEquals("owner", attachment.ownerId.value)
        assertEquals(AttachmentKind.LANDMARK, attachment.kind)
        assertEquals(listOf(request), source.existingRequests)
    }

    @Test
    fun `existing item not found maps to attachment not found`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Failed(
                CoreFailureReason.ATTACHMENT_NOT_FOUND,
                "attachment unavailable",
            )
        }

        val result = runtime(session, source).resolveExistingAttachment(
            session,
            ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("missing-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment unavailable", failure.redactedMessage)
    }

    @Test
    fun `existing item kind mismatch fails without leaking item ID`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Success(snapshot("texture-item", "owner", AttachmentKind.TEXTURE))
        }

        val result = runtime(session, source).resolveExistingAttachment(
            session,
            ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("texture-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment kind mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("texture-item"))
    }

    @Test
    fun `attachment runtime rejects mismatched session without calling source`() {
        val runtime = ProtocolInventoryRuntime(
            clientSession = LibomvClientSession.active(hostessSession("live-session")),
            inventorySource = FakeInventoryRuntimeSource(),
        )

        val result = runtime.resolveExistingAttachment(
            hostessSession("other-session"),
            ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("hostess session mismatch", failure.redactedMessage)
        assertFalse(failure.redactedMessage.orEmpty().contains("live-session"))
        assertFalse(failure.redactedMessage.orEmpty().contains("other-session"))
    }

    private fun runtime(
        session: HostessSession,
        source: FakeInventoryRuntimeSource,
    ): ProtocolInventoryRuntime = ProtocolInventoryRuntime(
        clientSession = LibomvClientSession.active(session),
        inventorySource = source,
    )

    private fun snapshot(
        itemId: String,
        ownerId: String,
        kind: AttachmentKind,
    ): LibomvAttachmentSnapshot = LibomvAttachmentSnapshot(itemId, ownerId, kind)

    private fun hostessSession(id: String = "live-session"): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )

    private class FakeInventoryRuntimeSource : InventoryRuntimeSource {
        var existingResult: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_NOT_FOUND,
            "attachment unavailable",
        )
        val existingRequests = mutableListOf<ExistingInventoryAttachment>()

        override fun resolveExistingAttachment(
            session: HostessSession,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult {
            existingRequests += request
            return existingResult
        }
    }
}
