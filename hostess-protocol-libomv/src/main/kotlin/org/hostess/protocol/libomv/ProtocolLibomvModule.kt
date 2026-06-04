package org.hostess.protocol.libomv

import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionPort

data class LibomvProtocolRuntime(
    val sessionPort: SessionPort,
    val groupPort: GroupPort,
    val inventoryPort: InventoryPort,
    val noticePort: NoticePort,
    val clientSession: LibomvClientSession,
    val protocolAvailable: Boolean,
)

object ProtocolLibomvModule {
    fun liveRuntime(): LibomvProtocolRuntime = runtimeFor(LibomvClientSession.inactive())

    private fun runtimeFor(clientSession: LibomvClientSession): LibomvProtocolRuntime = LibomvProtocolRuntime(
        sessionPort = LibomvSessionAdapter(clientSession),
        groupPort = LibomvGroupAdapter(clientSession),
        inventoryPort = LibomvInventoryAdapter(clientSession),
        noticePort = LibomvNoticeAdapter(clientSession),
        clientSession = clientSession,
        protocolAvailable = clientSession.isProtocolAvailable(),
    )
}
