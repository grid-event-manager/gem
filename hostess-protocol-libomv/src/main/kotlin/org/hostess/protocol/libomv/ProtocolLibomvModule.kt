package org.hostess.protocol.libomv

import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionPort
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime
import org.hostess.protocol.libomv.transport.OkHttpProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpClient

data class LibomvProtocolRuntime(
    val sessionPort: SessionPort,
    val groupPort: GroupPort,
    val inventoryPort: InventoryPort,
    val noticePort: NoticePort,
    val clientSession: LibomvClientSession,
    val protocolAvailable: Boolean,
)

object ProtocolLibomvModule {
    fun liveRuntime(): LibomvProtocolRuntime = liveRuntime(OkHttpProtocolHttpClient())

    internal fun liveRuntime(httpClient: ProtocolHttpClient): LibomvProtocolRuntime {
        val clientSession = LibomvClientSession.inactive()
        return runtimeFor(
            clientSession = clientSession,
            groupRuntime = ProtocolGroupRuntime(clientSession),
            inventoryRuntime = ProtocolInventoryRuntime(clientSession),
            loginRuntime = ProtocolLoginRuntime(clientSession, httpClient),
        )
    }

    private fun runtimeFor(
        clientSession: LibomvClientSession,
        groupRuntime: ProtocolGroupRuntime?,
        inventoryRuntime: ProtocolInventoryRuntime?,
        loginRuntime: ProtocolLoginRuntime?,
    ): LibomvProtocolRuntime = LibomvProtocolRuntime(
        sessionPort = LibomvSessionAdapter(clientSession, loginRuntime),
        groupPort = LibomvGroupAdapter(clientSession, groupRuntime),
        inventoryPort = LibomvInventoryAdapter(clientSession, inventoryRuntime),
        noticePort = LibomvNoticeAdapter(clientSession),
        clientSession = clientSession,
        protocolAvailable = clientSession.isProtocolAvailable(),
    )
}
