package org.hostess.core.ports

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.UploadTextureAttachment

interface InventoryPort {
    fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult

    fun createLandmarkAttachment(
        session: HostessSession,
        request: CreateLandmarkAttachment,
    ): AttachmentResolutionResult

    fun uploadTextureAttachment(
        session: HostessSession,
        request: UploadTextureAttachment,
    ): AttachmentResolutionResult
}

sealed interface AttachmentResolutionResult {
    data class Resolved(val attachment: AttachmentRef) : AttachmentResolutionResult
    data class Failed(val failure: CoreFailure) : AttachmentResolutionResult
}
