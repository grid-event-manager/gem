package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryPort
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime

class LibomvInventoryAdapter(
    internal val clientSession: LibomvClientSession,
    private val inventoryRuntime: ProtocolInventoryRuntime? = null,
) : InventoryPort {
    override fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        inventoryRuntime?.resolveExistingAttachment(session, request)
            ?: AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_NOT_FOUND))

    override fun createLandmarkAttachment(
        session: HostessSession,
        request: CreateLandmarkAttachment,
    ): AttachmentResolutionResult =
        inventoryRuntime?.createLandmarkAttachment(session, request)
            ?: AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_CREATE_FAILED))

    override fun uploadTextureAttachment(
        session: HostessSession,
        request: UploadTextureAttachment,
    ): AttachmentResolutionResult =
        inventoryRuntime?.uploadTextureAttachment(session, request)
            ?: AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED))
}
