package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryPort

class LibomvInventoryAdapter(
    internal val clientSession: LibomvClientSession,
) : InventoryPort {
    override fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_NOT_FOUND))

    override fun createLandmarkAttachment(
        session: HostessSession,
        request: CreateLandmarkAttachment,
    ): AttachmentResolutionResult =
        AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_CREATE_FAILED))

    override fun uploadTextureAttachment(
        session: HostessSession,
        request: UploadTextureAttachment,
    ): AttachmentResolutionResult =
        AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_UPLOAD_FAILED))
}
