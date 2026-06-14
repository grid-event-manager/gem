package org.gem.protocol.libomv

import java.nio.file.Path
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.GroupPort
import org.gem.core.ports.InventoryPort
import org.gem.core.ports.NoticePort
import org.gem.core.ports.SessionPort
import org.gem.protocol.libomv.runtime.CurrentOutfitVersionSource
import org.gem.protocol.libomv.runtime.CurrentGroupsSource
import org.gem.protocol.libomv.runtime.DefaultLibomvPlatformAdapterBundle
import org.gem.protocol.libomv.runtime.FileInventorySnapshotCacheStore
import org.gem.protocol.libomv.runtime.GroupNoticeArchiveSource
import org.gem.protocol.libomv.runtime.GemMachineIdentityProvider
import org.gem.protocol.libomv.runtime.GemViewerIdentityProvider
import org.gem.protocol.libomv.runtime.InventorySnapshotCacheStore
import org.gem.protocol.libomv.runtime.InventoryRuntimeSource
import org.gem.protocol.libomv.runtime.LibomvPlatformAdapterBundle
import org.gem.protocol.libomv.runtime.LoginSecretResolver
import org.gem.protocol.libomv.runtime.NoticeRuntimeSource
import org.gem.protocol.libomv.runtime.ProtocolAvatarAppearanceSource
import org.gem.protocol.libomv.runtime.ProtocolAvatarRuntime
import org.gem.protocol.libomv.runtime.ProtocolCurrentGroupsSource
import org.gem.protocol.libomv.runtime.ProtocolGroupNoticeArchiveSource
import org.gem.protocol.libomv.runtime.ProtocolGroupRuntime
import org.gem.protocol.libomv.runtime.ProtocolInventoryHttpSource
import org.gem.protocol.libomv.runtime.ProtocolInventoryRuntime
import org.gem.protocol.libomv.runtime.ProtocolLoginStartLocationProbe
import org.gem.protocol.libomv.runtime.ProtocolLoginRuntime
import org.gem.protocol.libomv.runtime.ProtocolNoticeCircuitSource
import org.gem.protocol.libomv.runtime.ProtocolNoticeRuntime
import org.gem.protocol.libomv.runtime.ProtocolSimulatorPresenceSource
import org.gem.protocol.libomv.runtime.SimulatorPresenceSource
import org.gem.protocol.libomv.transport.AgentDataUpdateRequestTransport
import org.gem.protocol.libomv.transport.EventQueueGetClient
import org.gem.protocol.libomv.transport.ProtocolCapabilityCacheProvider
import org.gem.protocol.libomv.transport.ProtocolCapabilitySeedClient

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

    fun liveRuntime(
        secretResolver: LoginSecretResolver,
        inventorySnapshotCacheDirectory: Path,
        viewerIdentityProvider: GemViewerIdentityProvider,
        machineIdentityProvider: GemMachineIdentityProvider,
    ): LibomvProtocolRuntime =
        liveRuntime(
            bundle = DefaultLibomvPlatformAdapterBundle.create(
                secretResolver = secretResolver,
                viewerIdentityProvider = viewerIdentityProvider,
                machineIdentityProvider = machineIdentityProvider,
            ),
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
                circuitClient = bundle.circuitSender,
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
