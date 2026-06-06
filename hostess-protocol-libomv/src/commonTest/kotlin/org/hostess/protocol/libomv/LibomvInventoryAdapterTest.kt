package org.hostess.protocol.libomv

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.LocalPosition
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import org.hostess.protocol.libomv.runtime.AttachmentPayloadResult
import org.hostess.protocol.libomv.runtime.AttachmentPayloadSource
import org.hostess.protocol.libomv.runtime.InventoryRuntimeResult
import org.hostess.protocol.libomv.runtime.InventoryRuntimeSource
import org.hostess.protocol.libomv.runtime.InventoryUploadResult
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

        val result = adapter.resolveExistingAttachment(
            session,
            request,
        )

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
    fun `landmark creation routes through protocol runtime`() {
        val session = hostessSession()
        val adapter = adapter(
            session = session,
            source = source(
                create = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK)),
            ),
        )

        val result = adapter.createLandmarkAttachment(
            session,
            CreateLandmarkAttachment("Venue", "region-id", LocalPosition(1.0, 2.0, 3.0)),
        )

        assertEquals(AttachmentKind.LANDMARK, assertIs<AttachmentResolutionResult.Resolved>(result).attachment.kind)
    }

    @Test
    fun `texture upload routes through protocol runtime`() {
        val session = hostessSession()
        val adapter = adapter(
            session = session,
            source = source(
                upload = InventoryUploadResult.Complete(snapshot("texture-item", "owner", AttachmentKind.TEXTURE)),
            ),
        )

        val result = adapter.uploadTextureAttachment(
            session,
            UploadTextureAttachment(
                fileName = "poster.png",
                contentDigest = "sha256:abc",
                payloadHandle = AttachmentPayloadHandle("texture-handle"),
            ),
        )

        assertEquals(AttachmentKind.TEXTURE, assertIs<AttachmentResolutionResult.Resolved>(result).attachment.kind)
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
                payloadSource = AttachmentPayloadSource { _, _ ->
                    AttachmentPayloadResult.Resolved(byteArrayOf(1, 2, 3), "sha256:abc")
                },
            ),
        )
    }

    private fun source(
        existing: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_NOT_FOUND,
            "attachment unavailable",
        ),
        create: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_CREATE_FAILED,
            "landmark attachment unavailable",
        ),
        upload: InventoryUploadResult = InventoryUploadResult.Failed(
            CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
            "texture upload unavailable",
        ),
    ): InventoryRuntimeSource = object : InventoryRuntimeSource {
        override fun resolveExistingAttachment(
            session: HostessSession,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult = existing

        override fun createLandmarkAttachment(
            session: HostessSession,
            request: CreateLandmarkAttachment,
            assetBytes: ByteArray,
        ): InventoryRuntimeResult = create

        override fun beginTextureUpload(
            session: HostessSession,
            request: UploadTextureAttachment,
            bytes: ByteArray,
        ): InventoryUploadResult = upload

        override fun completeTextureUpload(
            session: HostessSession,
            nextEndpoint: String,
            bytes: ByteArray,
        ): InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
            "texture upload unavailable",
        )
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
