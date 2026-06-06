package org.hostess.protocol.libomv

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.domain.SessionId
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryItemListResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.runtime.InventoryRuntimeResult
import org.hostess.protocol.libomv.runtime.InventoryRuntimeSource
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LibomvInventoryAdapterTest {
    @Test
    fun `existing attachment routes through protocol runtime`() {
        val session = hostessSession()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
        val adapter = adapter(
            session = session,
            source = source(
                existing = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK)),
            ),
        )

        val result = adapter.resolveExistingAttachment(session, request)

        assertEquals("landmark-item", assertIs<AttachmentResolutionResult.Resolved>(result).attachment.attachmentId.value)
    }

    @Test
    fun `existing attachment source failure maps through adapter`() {
        val session = hostessSession()
        val adapter = adapter(
            session = session,
            source = source(
                existing = InventoryRuntimeResult.Failed(
                    CoreFailureReason.ATTACHMENT_NOT_FOUND,
                    "attachment unavailable",
                ),
            ),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(
            adapter.resolveExistingAttachment(
                session,
                ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId("texture-item")),
            ),
        ).failure

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("attachment unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory adapter fallback still fails closed without runtime`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(hostessSession()))

        val failure = assertIs<AttachmentResolutionResult.Failed>(
            adapter.resolveExistingAttachment(
                hostessSession(),
                ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item")),
            ),
        ).failure

        assertEquals(CoreFailureReason.ATTACHMENT_NOT_FOUND, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    @Test
    fun `inventory list fails closed until protocol catalogue runtime lands`() {
        val adapter = LibomvInventoryAdapter(clientSession = LibomvClientSession.active(hostessSession()))

        val failure = assertIs<InventoryItemListResult.Failure>(
            adapter.listItems(hostessSession(), InventoryItemQuery()),
        ).failure

        assertEquals(CoreFailureReason.INVENTORY_LIST_FAILED, failure.reason)
        assertEquals("protocol runtime unavailable", failure.redactedMessage)
    }

    private fun adapter(
        session: HostessSession,
        source: InventoryRuntimeSource,
    ): LibomvInventoryAdapter {
        val clientSession = LibomvClientSession.active(session)
        return LibomvInventoryAdapter(
            clientSession = clientSession,
            inventoryRuntime = ProtocolInventoryRuntime(
                clientSession = clientSession,
                inventorySource = source,
            ),
        )
    }

    private fun source(
        existing: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_NOT_FOUND,
            "attachment unavailable",
        ),
    ): InventoryRuntimeSource = object : InventoryRuntimeSource {
        override fun resolveExistingAttachment(
            session: HostessSession,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult = existing
    }

    private fun snapshot(
        itemId: String,
        ownerId: String,
        kind: AttachmentKind,
    ): LibomvAttachmentSnapshot = LibomvAttachmentSnapshot(itemId, ownerId, kind)

    private fun hostessSession(): HostessSession = HostessSession(
        sessionId = SessionId("live-session"),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
        isActive = true,
    )
}
