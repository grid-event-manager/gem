package org.hostess.protocol.libomv

import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionPort
import org.hostess.protocol.libomv.runtime.CurrentGroupsSource
import org.hostess.protocol.libomv.runtime.DefaultLibomvPlatformAdapterBundle
import org.hostess.protocol.libomv.runtime.LibomvPlatformAdapterBundle
import org.hostess.protocol.libomv.runtime.ProtocolCurrentGroupsSource
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime
import org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequestTransport
import org.hostess.protocol.libomv.transport.EventQueueGetClient
import org.hostess.protocol.libomv.transport.ProtocolCapabilityCacheProvider
import org.hostess.protocol.libomv.transport.ProtocolCapabilitySeedClient

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
    fun liveRuntime(): LibomvProtocolRuntime =
        liveRuntime(DefaultLibomvPlatformAdapterBundle.create())

    internal fun liveRuntime(bundle: LibomvPlatformAdapterBundle): LibomvProtocolRuntime {
        val clientSession = if (bundle.adapterLoad) LibomvClientSession.inactive() else LibomvClientSession.unavailable()
        val runtimeReady = bundle.adapterLoad && bundle.runtimeLoad
        val currentGroupsSource = if (bundle.transportLoad) {
            val capabilitySeedClient = ProtocolCapabilitySeedClient(bundle.httpClient)
            ProtocolCurrentGroupsSource(
                capabilityUrlProvider = ProtocolCapabilityCacheProvider(
                    clientSession = clientSession,
                    seedClient = capabilitySeedClient,
                ),
                eventQueueGetClient = EventQueueGetClient(bundle.httpClient),
                requestTransport = AgentDataUpdateRequestTransport(bundle.circuitSender),
            )
        } else {
            CurrentGroupsSource.unavailable()
        }
        val groupRuntime = if (runtimeReady) ProtocolGroupRuntime(clientSession, currentGroupsSource) else null
        val inventoryRuntime = if (runtimeReady) ProtocolInventoryRuntime(clientSession) else null
        val loginRuntime = if (runtimeReady) {
            ProtocolLoginRuntime(
                clientSession = clientSession,
                httpClient = bundle.httpClient,
                viewerIdentityProvider = bundle.viewerIdentityProvider,
                secretResolver = bundle.secretResolver,
                clockPort = bundle.clockPort,
                machineIdentityProvider = bundle.machineIdentityProvider,
                digestPort = bundle.md5DigestPort,
            )
        } else {
            null
        }
        val noticeRuntime = if (runtimeReady) ProtocolNoticeRuntime(clientSession) else null
        return runtimeFor(
            clientSession = clientSession,
            groupRuntime = groupRuntime,
            inventoryRuntime = inventoryRuntime,
            loginRuntime = loginRuntime,
            noticeRuntime = noticeRuntime,
            loadState = LibomvProtocolLoadState(
                adapterLoad = bundle.adapterLoad,
                runtimeLoad = bundle.runtimeLoad,
                transportLoad = bundle.transportLoad,
            ),
        )
    }

    private fun runtimeFor(
        clientSession: LibomvClientSession,
        groupRuntime: ProtocolGroupRuntime?,
        inventoryRuntime: ProtocolInventoryRuntime?,
        loginRuntime: ProtocolLoginRuntime?,
        noticeRuntime: ProtocolNoticeRuntime?,
        loadState: LibomvProtocolLoadState,
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
            loadState = loadState,
        )
    }
}
