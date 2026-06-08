package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.llsd.LlsdXml
import org.hostess.protocol.libomv.llsd.asBoolean
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse

internal class ProtocolAvatarAppearanceSource(
    private val httpClient: ProtocolHttpClient,
) : AvatarAppearanceSource {
    override fun updateServerAppearance(
        identity: LibomvSessionIdentity,
        cofVersion: Int,
        capabilityUrl: CapabilityUrl,
    ): AvatarAppearanceUpdateResult =
        try {
            httpClient.execute(updateRequest(capabilityUrl, cofVersion)).toUpdateResult()
        } catch (ex: ProtocolHttpException) {
            AvatarAppearanceUpdateResult.TransportGap("avatar appearance transport unavailable")
        }

    private fun ProtocolHttpResponse.toUpdateResult(): AvatarAppearanceUpdateResult {
        if (statusCode !in 200..299) {
            return AvatarAppearanceUpdateResult.TransportGap("avatar appearance transport unavailable")
        }
        val fields = LlsdXml.parseMap(body)
            ?: return AvatarAppearanceUpdateResult.ProofGap("avatar appearance response invalid")
        if (fields[ERROR_KEY] != null) {
            return AvatarAppearanceUpdateResult.ProofGap("avatar appearance response invalid")
        }
        return when (fields[SUCCESS_KEY]?.asBoolean()) {
            true -> AvatarAppearanceUpdateResult.Success
            false,
            null,
            -> AvatarAppearanceUpdateResult.ProofGap("avatar appearance response invalid")
        }
    }

    private fun updateRequest(
        capabilityUrl: CapabilityUrl,
        cofVersion: Int,
    ): ProtocolHttpRequest = ProtocolHttpRequest(
        method = "POST",
        url = capabilityUrl.value,
        headers = mapOf("Content-Type" to LLSD_XML),
        body = ProtocolHttpBody.TextBody(updateBody(cofVersion), LLSD_XML),
    )

    private fun updateBody(cofVersion: Int): String =
        "<llsd><map><key>$COF_VERSION_KEY</key><integer>$cofVersion</integer></map></llsd>"

    private companion object {
        const val LLSD_XML = "application/llsd+xml"
        const val COF_VERSION_KEY = "cof_version"
        const val SUCCESS_KEY = "success"
        const val ERROR_KEY = "error"
    }
}
