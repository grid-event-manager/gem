package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity

internal fun interface CapabilityUrlProvider {
    fun requireUrl(
        identity: LibomvSessionIdentity,
        name: CapabilityName,
    ): CapabilityUrlResult
}

internal class ProtocolCapabilityCacheProvider(
    private val clientSession: LibomvClientSession,
    private val seedClient: ProtocolCapabilitySeedClient,
) : CapabilityUrlProvider {
    override fun requireUrl(
        identity: LibomvSessionIdentity,
        name: CapabilityName,
    ): CapabilityUrlResult {
        val cache = clientSession.capabilityCache(identity)
            ?: return CapabilityUrlResult.TransportGap("capability cache unavailable")
        cache.urlFor(name)?.let { return CapabilityUrlResult.Ready(it) }

        val seeded = when (val result = seedClient.seed(identity.seedCapability, PRODUCTION_CAPABILITY_REQUESTS)) {
            is CapabilitySeedResult.Seeded -> result.urls
            is CapabilitySeedResult.TransportGap -> return CapabilityUrlResult.TransportGap(result.redactedMessage)
            is CapabilitySeedResult.MappingGap -> return CapabilityUrlResult.MappingGap(result.redactedMessage)
        }
        val updatedCache = cache.withUrls(seeded)
        if (!clientSession.replaceCapabilityCache(identity, updatedCache)) {
            return CapabilityUrlResult.TransportGap("capability cache unavailable")
        }
        return updatedCache.urlFor(name)?.let(CapabilityUrlResult::Ready)
            ?: CapabilityUrlResult.MappingGap("capability url absent: ${name.wireName}")
    }

    companion object {
        val PRODUCTION_CAPABILITY_REQUESTS: Set<CapabilityName> = setOf(
            CapabilityName.EVENT_QUEUE_GET,
            CapabilityName.FETCH_INVENTORY2,
            CapabilityName.FETCH_INVENTORY_DESCENDENTS2,
        )
    }
}
