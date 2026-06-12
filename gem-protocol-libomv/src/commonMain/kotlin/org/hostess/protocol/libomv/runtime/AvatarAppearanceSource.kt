package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.CapabilityUrl

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
