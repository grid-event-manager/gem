package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.LocalPosition
import org.hostess.core.domain.SessionId
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.mapping.LibomvAttachmentSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtocolInventoryRuntimeTest {
    @Test
    fun `existing item resolves when kind matches`() {
        val session = hostessSession()
        val request = ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId("landmark-item"))
        val source = FakeInventoryRuntimeSource().apply {
            existingResult = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK))
        }

        val result = runtime(session, source).resolveExistingAttachment(
            session,
            request,
        )

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
    fun `landmark creation uses source landmark asset format`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            createResult = InventoryRuntimeResult.Success(snapshot("landmark-item", "owner", AttachmentKind.LANDMARK))
        }
        val request = CreateLandmarkAttachment(
            venueLabel = "Venue",
            regionId = "region-id",
            localPosition = LocalPosition(1.0, 2.0, 3.0),
        )

        val result = runtime(session, source).createLandmarkAttachment(session, request)

        assertIs<AttachmentResolutionResult.Resolved>(result)
        assertEquals(
            "Landmark version 2\nregion_id region-id\nlocal_pos 1.000000 2.000000 3.000000\n",
            source.landmarkAssetBytes.single().decodeToString(),
        )
    }

    @Test
    fun `texture digest mismatch fails before upload source`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource()
        val payloadSource = FakeAttachmentPayloadSource(actualDigest = "sha256:actual")

        val result = runtime(session, source, payloadSource).uploadTextureAttachment(
            session,
            textureRequest(expectedDigest = "sha256:expected"),
        )

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, failure.reason)
        assertEquals("texture payload digest mismatch", failure.redactedMessage)
        assertTrue(source.textureBeginRequests.isEmpty())
    }

    @Test
    fun `texture upload continue state completes through second source call`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            uploadBeginResult = InventoryUploadResult.Continue("endpoint-token")
            uploadCompleteResult = InventoryRuntimeResult.Success(snapshot("texture-item", "owner", AttachmentKind.TEXTURE))
        }

        val result = runtime(session, source).uploadTextureAttachment(session, textureRequest())

        val attachment = assertIs<AttachmentResolutionResult.Resolved>(result).attachment
        assertEquals("texture-item", attachment.attachmentId.value)
        assertEquals(listOf("endpoint-token"), source.textureCompleteEndpoints)
    }

    @Test
    fun `texture upload complete state resolves without second source call`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            uploadBeginResult = InventoryUploadResult.Complete(snapshot("texture-item", "owner", AttachmentKind.TEXTURE))
        }

        val result = runtime(session, source).uploadTextureAttachment(session, textureRequest())

        assertIs<AttachmentResolutionResult.Resolved>(result)
        assertTrue(source.textureCompleteEndpoints.isEmpty())
    }

    @Test
    fun `texture upload fails when continue state has no endpoint`() {
        val session = hostessSession()
        val source = FakeInventoryRuntimeSource().apply {
            uploadBeginResult = InventoryUploadResult.Continue(" ")
        }

        val result = runtime(session, source).uploadTextureAttachment(session, textureRequest())

        val failure = assertIs<AttachmentResolutionResult.Failed>(result).failure
        assertEquals(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED, failure.reason)
        assertEquals("texture upload endpoint unavailable", failure.redactedMessage)
        assertTrue(source.textureCompleteEndpoints.isEmpty())
    }

    @Test
    fun `attachment runtime rejects mismatched session without calling source`() {
        val runtime = ProtocolInventoryRuntime(
            clientSession = LibomvClientSession.active(hostessSession("live-session")),
            inventorySource = FakeInventoryRuntimeSource(),
            payloadSource = FakeAttachmentPayloadSource(),
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
        payloadSource: AttachmentPayloadSource = FakeAttachmentPayloadSource(),
    ): ProtocolInventoryRuntime = ProtocolInventoryRuntime(
        clientSession = LibomvClientSession.active(session),
        inventorySource = source,
        payloadSource = payloadSource,
    )

    private fun snapshot(
        itemId: String,
        ownerId: String,
        kind: AttachmentKind,
    ): LibomvAttachmentSnapshot = LibomvAttachmentSnapshot(itemId, ownerId, kind)

    private fun textureRequest(expectedDigest: String = "sha256:abc"): UploadTextureAttachment = UploadTextureAttachment(
        fileName = "poster.png",
        contentDigest = expectedDigest,
        payloadHandle = AttachmentPayloadHandle("texture-handle"),
    )

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
        var createResult: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_CREATE_FAILED,
            "landmark attachment unavailable",
        )
        var uploadBeginResult: InventoryUploadResult = InventoryUploadResult.Failed(
            CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
            "texture upload unavailable",
        )
        var uploadCompleteResult: InventoryRuntimeResult = InventoryRuntimeResult.Failed(
            CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
            "texture upload unavailable",
        )
        val existingRequests = mutableListOf<ExistingInventoryAttachment>()
        val landmarkAssetBytes = mutableListOf<ByteArray>()
        val textureBeginRequests = mutableListOf<UploadTextureAttachment>()
        val textureCompleteEndpoints = mutableListOf<String>()

        override fun resolveExistingAttachment(
            session: HostessSession,
            request: ExistingInventoryAttachment,
        ): InventoryRuntimeResult {
            existingRequests += request
            return existingResult
        }

        override fun createLandmarkAttachment(
            session: HostessSession,
            request: CreateLandmarkAttachment,
            assetBytes: ByteArray,
        ): InventoryRuntimeResult {
            landmarkAssetBytes += assetBytes
            return createResult
        }

        override fun beginTextureUpload(
            session: HostessSession,
            request: UploadTextureAttachment,
            bytes: ByteArray,
        ): InventoryUploadResult {
            textureBeginRequests += request
            return uploadBeginResult
        }

        override fun completeTextureUpload(
            session: HostessSession,
            nextEndpoint: String,
            bytes: ByteArray,
        ): InventoryRuntimeResult {
            textureCompleteEndpoints += nextEndpoint
            return uploadCompleteResult
        }
    }

    private class FakeAttachmentPayloadSource(
        private val actualDigest: String = "sha256:abc",
    ) : AttachmentPayloadSource {
        override fun open(handle: AttachmentPayloadHandle, expectedDigest: String): AttachmentPayloadResult {
            if (actualDigest != expectedDigest) {
                return AttachmentPayloadResult.Failed(
                    CoreFailure(
                        CoreFailureReason.ATTACHMENT_UPLOAD_FAILED,
                        redactedMessage = "texture payload digest mismatch",
                    ),
                )
            }
            return AttachmentPayloadResult.Resolved(byteArrayOf(1, 2, 3), actualDigest)
        }
    }
}
