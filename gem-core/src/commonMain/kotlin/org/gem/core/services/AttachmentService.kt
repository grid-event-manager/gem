package org.gem.core.services

import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemSession
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.InventoryPort

class AttachmentService(
    private val inventoryPort: InventoryPort,
) {
    fun resolveAttachment(
        session: GemSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult = inventoryPort.resolveExistingAttachment(session, request)
}
