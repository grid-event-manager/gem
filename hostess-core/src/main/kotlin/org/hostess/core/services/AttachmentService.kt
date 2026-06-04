package org.hostess.core.services

import org.hostess.core.domain.AttachmentRequest
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryPort

class AttachmentService(
    private val inventoryPort: InventoryPort,
) {
    fun resolveAttachment(
        session: HostessSession,
        request: AttachmentRequest,
    ): AttachmentResolutionResult = when (request) {
        is ExistingInventoryAttachment -> inventoryPort.resolveExistingAttachment(session, request)
        is CreateLandmarkAttachment -> inventoryPort.createLandmarkAttachment(session, request)
        is UploadTextureAttachment -> inventoryPort.uploadTextureAttachment(session, request)
    }
}
