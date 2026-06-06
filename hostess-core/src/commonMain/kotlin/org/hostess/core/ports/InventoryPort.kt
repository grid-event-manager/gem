package org.hostess.core.ports

import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemDescriptor
import org.hostess.core.domain.InventoryItemQuery

interface InventoryPort {
    fun resolveExistingAttachment(
        session: HostessSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult

    fun listItems(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult
}

sealed interface AttachmentResolutionResult {
    data class Resolved(val attachment: AttachmentRef) : AttachmentResolutionResult
    data class Failed(val failure: CoreFailure) : AttachmentResolutionResult
}

sealed interface InventoryItemListResult {
    data class Success(val items: List<InventoryItemDescriptor>) : InventoryItemListResult
    data class Failure(val failure: CoreFailure) : InventoryItemListResult
}
