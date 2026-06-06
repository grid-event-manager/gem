package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.InventoryItemQuery
import org.hostess.core.ports.AttachmentResolutionResult
import org.hostess.core.ports.InventoryItemListResult
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

    override fun listItems(
        session: HostessSession,
        query: InventoryItemQuery,
    ): InventoryItemListResult =
        inventoryRuntime?.listItems(session, query)
            ?: InventoryItemListResult.Failure(clientSession.unavailable(CoreFailureReason.INVENTORY_LIST_FAILED))
}
