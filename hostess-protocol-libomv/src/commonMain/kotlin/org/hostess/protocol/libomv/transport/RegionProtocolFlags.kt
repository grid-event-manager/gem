package org.hostess.protocol.libomv.transport

internal data class RegionProtocolFlags(
    val agentAppearanceService: Boolean,
) {
    companion object {
        fun unknown(): RegionProtocolFlags = RegionProtocolFlags(agentAppearanceService = false)
    }
}

internal data class RegionHandshakeInfo(
    val regionProtocolFlags: RegionProtocolFlags,
)
