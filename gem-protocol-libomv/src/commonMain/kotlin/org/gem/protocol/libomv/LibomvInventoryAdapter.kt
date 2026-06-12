package org.gem.protocol.libomv

import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.ExistingInventoryAttachment
import org.gem.core.domain.GemSession
import org.gem.core.domain.InventoryItemQuery
import org.gem.core.ports.AttachmentResolutionResult
import org.gem.core.ports.InventoryDirectoryListResult
import org.gem.core.ports.InventoryItemListResult
import org.gem.core.ports.InventoryPort
import org.gem.protocol.libomv.runtime.ProtocolInventoryRuntime

class LibomvInventoryAdapter(
    internal val clientSession: LibomvClientSession,
    private val inventoryRuntime: ProtocolInventoryRuntime? = null,
) : InventoryPort {
    override fun resolveExistingAttachment(
        session: GemSession,
        request: ExistingInventoryAttachment,
    ): AttachmentResolutionResult =
        inventoryRuntime?.resolveExistingAttachment(session, request)
            ?: AttachmentResolutionResult.Failed(clientSession.unavailable(CoreFailureReason.ATTACHMENT_NOT_FOUND))

    override fun listDirectory(
        session: GemSession,
        query: InventoryItemQuery,
    ): InventoryDirectoryListResult =
        inventoryRuntime?.listDirectory(session, query)
            ?: InventoryDirectoryListResult.Failure(clientSession.unavailable(CoreFailureReason.INVENTORY_LIST_FAILED))
}
