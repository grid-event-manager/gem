package org.gem.protocol.libomv.transport

@JvmInline
internal value class CapabilityUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "capability url must not be blank" }
    }
}

internal data class CapabilityCache(
    private val urls: Map<CapabilityName, CapabilityUrl>,
) {
    fun urlFor(name: CapabilityName): CapabilityUrl? = urls[name]

    fun withUrls(newUrls: Map<CapabilityName, CapabilityUrl>): CapabilityCache =
        CapabilityCache(urls + newUrls)

    companion object {
        fun empty(): CapabilityCache = CapabilityCache(emptyMap())
    }
}

internal sealed interface CapabilitySeedResult {
    data class Seeded(val urls: Map<CapabilityName, CapabilityUrl>) : CapabilitySeedResult
    data class TransportGap(val redactedMessage: String) : CapabilitySeedResult
    data class MappingGap(val redactedMessage: String) : CapabilitySeedResult
}

internal sealed interface CapabilityUrlResult {
    data class Ready(val url: CapabilityUrl) : CapabilityUrlResult
    data class TransportGap(val redactedMessage: String) : CapabilityUrlResult
    data class MappingGap(val redactedMessage: String) : CapabilityUrlResult
}
