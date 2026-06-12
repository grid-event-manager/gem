package org.gem.core.ports

import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.CoreFailure
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryDirectoryListing
import org.gem.core.domain.InventoryItemDescriptor
import org.gem.core.domain.InventoryItemQuery

interface InventoryPort {
    fun resolveExistingAttachment(
        session: GemSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult

    fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult

    fun listItems(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult =
        when (val result = listDirectory(session, query)) {
            is InventoryDirectoryListResult.Success -> InventoryItemListResult.Success(result.listing.items)
            is InventoryDirectoryListResult.Failure -> InventoryItemListResult.Failure(result.failure)
        }
}

sealed interface AttachmentResolutionResult {
    data class Resolved(val attachment: AttachmentRef) : AttachmentResolutionResult
    data class Failed(val failure: CoreFailure) : AttachmentResolutionResult
}

sealed interface InventoryItemListResult {
    data class Success(val items: List<InventoryItemDescriptor>) : InventoryItemListResult
    data class Failure(val failure: CoreFailure) : InventoryItemListResult
}

sealed interface InventoryDirectoryListResult {
    data class Success(val listing: InventoryDirectoryListing) : InventoryDirectoryListResult
    data class Failure(val failure: CoreFailure) : InventoryDirectoryListResult
}
