package org.gem.protocol.libomv.runtime

import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.transport.CapabilityUrl

internal fun interface AvatarAppearanceSource {
    fun updateServerAppearance(
        identity: LibomvSessionIdentity,
        cofVersion: Int,
        capabilityUrl: CapabilityUrl,
    ): AvatarAppearanceUpdateResult

    companion object {
        fun unavailable(): AvatarAppearanceSource = AvatarAppearanceSource { _, _, _ ->
            AvatarAppearanceUpdateResult.TransportGap("avatar appearance source unavailable")
        }
    }
}

internal sealed interface AvatarAppearanceUpdateResult {
    data object Success : AvatarAppearanceUpdateResult
    data class TransportGap(val redactedMessage: String) : AvatarAppearanceUpdateResult
    data class ProofGap(val redactedMessage: String) : AvatarAppearanceUpdateResult
}
