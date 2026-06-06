package org.hostess.core.services

import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryPort

class AttachmentService(
    private val inventoryPort: InventoryPort,
) {
    fun resolveAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult = inventoryPort.resolveExistingAttachment(session, request)
}
