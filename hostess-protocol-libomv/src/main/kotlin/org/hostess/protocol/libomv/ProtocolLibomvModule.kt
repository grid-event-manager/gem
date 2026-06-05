package org.hostess.protocol.libomv

import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionPort
import org.hostess.protocol.libomv.runtime.AttachmentPayloadResult
import org.hostess.protocol.libomv.runtime.AttachmentPayloadSource
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime
import org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime
import org.hostess.protocol.libomv.transport.OkHttpProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpClient

data class LibomvProtocolRuntime(
    val sessionPort: SessionPort,
    val groupPort: GroupPort,
    val inventoryPort: InventoryPort,
    val noticePort: NoticePort,
    val clientSession: LibomvClientSession,
    val protocolAvailable: Boolean,
    val loadState: LibomvProtocolLoadState,
)

data class LibomvProtocolLoadState(
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
)

object ProtocolLibomvModule {
    fun liveRuntime(): LibomvProtocolRuntime = liveRuntime(OkHttpProtocolHttpClient())

    internal fun liveRuntime(httpClient: ProtocolHttpClient): LibomvProtocolRuntime {
        val clientSession = LibomvClientSession.inactive()
        val groupRuntime = ProtocolGroupRuntime(clientSession)
        val inventoryRuntime = ProtocolInventoryRuntime(clientSession)
        val loginRuntime = ProtocolLoginRuntime(clientSession, httpClient)
        val noticeRuntime = ProtocolNoticeRuntime(clientSession)
        return runtimeFor(
            clientSession = clientSession,
            groupRuntime = groupRuntime,
            inventoryRuntime = inventoryRuntime,
            loginRuntime = loginRuntime,
            noticeRuntime = noticeRuntime,
            transportLoad = listOf(ProtocolHttpClient::class.java, httpClient::class.java).classesLoaded(),
        )
    }

    private fun runtimeFor(
        clientSession: LibomvClientSession,
        groupRuntime: ProtocolGroupRuntime?,
        inventoryRuntime: ProtocolInventoryRuntime?,
        loginRuntime: ProtocolLoginRuntime?,
        noticeRuntime: ProtocolNoticeRuntime?,
        transportLoad: Boolean,
    ): LibomvProtocolRuntime {
        val sessionAdapter = LibomvSessionAdapter(clientSession, loginRuntime)
        val groupAdapter = LibomvGroupAdapter(clientSession, groupRuntime)
        val inventoryAdapter = LibomvInventoryAdapter(clientSession, inventoryRuntime)
        val noticeAdapter = LibomvNoticeAdapter(clientSession, noticeRuntime)
        return LibomvProtocolRuntime(
            sessionPort = sessionAdapter,
            groupPort = groupAdapter,
            inventoryPort = inventoryAdapter,
            noticePort = noticeAdapter,
            clientSession = clientSession,
            protocolAvailable = clientSession.isProtocolAvailable(),
            loadState = LibomvProtocolLoadState(
                adapterLoad = listOf(
                    clientSession::class.java,
                    sessionAdapter::class.java,
                    groupAdapter::class.java,
                    inventoryAdapter::class.java,
                    noticeAdapter::class.java,
                ).classesLoaded(),
                runtimeLoad = listOf(
                    loginRuntime?.let { it::class.java },
                    groupRuntime?.let { it::class.java },
                    inventoryRuntime?.let { it::class.java },
                    noticeRuntime?.let { it::class.java },
                    AttachmentPayloadSource::class.java,
                    AttachmentPayloadResult::class.java,
                ).classesLoaded(),
                transportLoad = transportLoad,
            ),
        )
    }

    private fun List<Class<*>?>.classesLoaded(): Boolean = all { it?.name?.isNotBlank() == true }
}
