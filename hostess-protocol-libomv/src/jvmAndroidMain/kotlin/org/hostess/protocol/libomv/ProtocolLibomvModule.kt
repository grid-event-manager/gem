package org.hostess.protocol.libomv

import java.nio.file.Path
import org.hostess.core.ports.AvatarPort
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.InventoryPort
import org.hostess.core.ports.NoticePort
import org.hostess.core.ports.SessionPort
import org.hostess.protocol.libomv.runtime.CurrentOutfitVersionSource
import org.hostess.protocol.libomv.runtime.CurrentGroupsSource
import org.hostess.protocol.libomv.runtime.DefaultLibomvPlatformAdapterBundle
import org.hostess.protocol.libomv.runtime.FileInventorySnapshotCacheStore
import org.hostess.protocol.libomv.runtime.GroupNoticeArchiveSource
import org.hostess.protocol.libomv.runtime.InventorySnapshotCacheStore
import org.hostess.protocol.libomv.runtime.InventoryRuntimeSource
import org.hostess.protocol.libomv.runtime.LibomvPlatformAdapterBundle
import org.hostess.protocol.libomv.runtime.LoginSecretResolver
import org.hostess.protocol.libomv.runtime.NoticeRuntimeSource
import org.hostess.protocol.libomv.runtime.ProtocolAvatarAppearanceSource
import org.hostess.protocol.libomv.runtime.ProtocolAvatarRuntime
import org.hostess.protocol.libomv.runtime.ProtocolCurrentGroupsSource
import org.hostess.protocol.libomv.runtime.ProtocolGroupNoticeArchiveSource
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime
import org.hostess.protocol.libomv.runtime.ProtocolInventoryHttpSource
import org.hostess.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.hostess.protocol.libomv.runtime.ProtocolLoginStartLocationProbe
import org.hostess.protocol.libomv.runtime.ProtocolLoginRuntime
import org.hostess.protocol.libomv.runtime.ProtocolNoticeCircuitSource
import org.hostess.protocol.libomv.runtime.ProtocolNoticeRuntime
import org.hostess.protocol.libomv.runtime.ProtocolSimulatorPresenceSource
import org.hostess.protocol.libomv.runtime.SimulatorPresenceSource
import org.hostess.protocol.libomv.transport.AgentDataUpdateRequestTransport
import org.hostess.protocol.libomv.transport.EventQueueGetClient
import org.hostess.protocol.libomv.transport.ProtocolCapabilityCacheProvider
import org.hostess.protocol.libomv.transport.ProtocolCapabilitySeedClient

data class LibomvProtocolRuntime(
    val sessionPort: SessionPort,
    val groupPort: GroupPort,
    val inventoryPort: InventoryPort,
    val noticePort: NoticePort,
    val avatarPort: AvatarPort,
    val loginStartLocationProbe: ProtocolLoginStartLocationProbe,
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

    fun liveRuntime(secretResolver: LoginSecretResolver): LibomvProtocolRuntime =
        liveRuntime(DefaultLibomvPlatformAdapterBundle.create(secretResolver))

    fun liveRuntime(
        secretResolver: LoginSecretResolver,
        inventorySnapshotCacheDirectory: Path,
    ): LibomvProtocolRuntime =
        liveRuntime(
            bundle = DefaultLibomvPlatformAdapterBundle.create(secretResolver),
            inventorySnapshotCacheStore = FileInventorySnapshotCacheStore(inventorySnapshotCacheDirectory),
        )

    internal fun liveRuntime(bundle: LibomvPlatformAdapterBundle): LibomvProtocolRuntime {
        return liveRuntime(bundle, InventorySnapshotCacheStore.unavailable())
    }

    internal fun liveRuntime(
        bundle: LibomvPlatformAdapterBundle,
        inventorySnapshotCacheStore: InventorySnapshotCacheStore,
    ): LibomvProtocolRuntime {
        val clientSession = if (bundle.adapterLoad) LibomvClientSession.inactive() else LibomvClientSession.unavailable()
        val runtimeReady = bundle.adapterLoad && bundle.runtimeLoad
        val capabilityProvider = if (bundle.transportLoad) {
            ProtocolCapabilityCacheProvider(
                clientSession = clientSession,
                seedClient = ProtocolCapabilitySeedClient(bundle.httpClient),
            )
        } else {
            null
        }
        val currentGroupsSource = if (bundle.transportLoad) {
            ProtocolCurrentGroupsSource(
                capabilityUrlProvider = requireNotNull(capabilityProvider),
                eventQueueGetClient = EventQueueGetClient(bundle.httpClient),
                requestTransport = AgentDataUpdateRequestTransport(bundle.circuitSender),
            )
        } else {
            CurrentGroupsSource.unavailable()
        }
        val simulatorPresenceSource = if (bundle.transportLoad) {
            ProtocolSimulatorPresenceSource(bundle.circuitSender)
        } else {
            SimulatorPresenceSource.unavailable()
        }
        val noticeArchiveSource = if (bundle.transportLoad) {
            ProtocolGroupNoticeArchiveSource(bundle.circuitSender)
        } else {
            GroupNoticeArchiveSource.unavailable()
        }
        val groupRuntime = if (runtimeReady) {
            ProtocolGroupRuntime(
                clientSession = clientSession,
                currentGroupsSource = currentGroupsSource,
                simulatorPresenceSource = simulatorPresenceSource,
                noticeArchiveSource = noticeArchiveSource,
            )
        } else {
            null
        }
        val inventoryRuntime = if (runtimeReady) {
            ProtocolInventoryRuntime(
                clientSession = clientSession,
                capabilityUrlProvider = capabilityProvider,
                inventorySource = if (capabilityProvider != null) {
                    ProtocolInventoryHttpSource(
                        httpClient = bundle.httpClient,
                        snapshotCacheStore = inventorySnapshotCacheStore,
                    )
                } else {
                    InventoryRuntimeSource.unavailable()
                },
            )
        } else {
            null
        }
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
        val noticeRuntime = if (runtimeReady) {
            ProtocolNoticeRuntime(
                clientSession = clientSession,
                noticeSource = if (bundle.transportLoad) {
                    ProtocolNoticeCircuitSource(bundle.circuitSender)
                } else {
                    NoticeRuntimeSource.unavailable()
                },
            )
        } else {
            null
        }
        val avatarRuntime = if (runtimeReady && bundle.transportLoad && capabilityProvider != null) {
            ProtocolAvatarRuntime(
                clientSession = clientSession,
                circuitClient = bundle.circuitSender,
                capabilityUrlProvider = capabilityProvider,
                currentOutfitVersionSource = CurrentOutfitVersionSource(),
                appearanceSource = ProtocolAvatarAppearanceSource(bundle.httpClient),
            )
        } else {
            null
        }
        return runtimeFor(
            clientSession = clientSession,
            groupRuntime = groupRuntime,
            inventoryRuntime = inventoryRuntime,
            loginRuntime = loginRuntime,
            noticeRuntime = noticeRuntime,
            avatarRuntime = avatarRuntime,
            loginStartLocationProbe = if (runtimeReady) {
                ProtocolLoginStartLocationProbe(bundle.secretResolver)
            } else {
                ProtocolLoginStartLocationProbe.unavailable()
            },
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
        avatarRuntime: ProtocolAvatarRuntime?,
        loginStartLocationProbe: ProtocolLoginStartLocationProbe,
        loadState: LibomvProtocolLoadState,
    ): LibomvProtocolRuntime {
        val sessionAdapter = LibomvSessionAdapter(clientSession, loginRuntime)
        val groupAdapter = LibomvGroupAdapter(clientSession, groupRuntime)
        val inventoryAdapter = LibomvInventoryAdapter(clientSession, inventoryRuntime)
        val noticeAdapter = LibomvNoticeAdapter(clientSession, noticeRuntime)
        val avatarAdapter = LibomvAvatarAdapter(avatarRuntime)
        return LibomvProtocolRuntime(
            sessionPort = sessionAdapter,
            groupPort = groupAdapter,
            inventoryPort = inventoryAdapter,
            noticePort = noticeAdapter,
            avatarPort = avatarAdapter,
            loginStartLocationProbe = loginStartLocationProbe,
            clientSession = clientSession,
            protocolAvailable = clientSession.isProtocolAvailable(),
            loadState = loadState,
        )
    }
}
