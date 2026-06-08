package org.hostess.protocol.libomv.runtime

import org.hostess.core.services.SafeDiagnosticRedaction
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.llsd.LlsdValue
import org.hostess.protocol.libomv.llsd.LlsdXml
import org.hostess.protocol.libomv.llsd.asBoolean
import org.hostess.protocol.libomv.llsd.asLong
import org.hostess.protocol.libomv.llsd.asString
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
            return AvatarAppearanceUpdateResult.TransportGap(
                "avatar appearance transport unavailable: http_status=$statusCode; ${responseDiagnostic()}",
            )
        }
        val fields = LlsdXml.parseMap(body)
            ?: return AvatarAppearanceUpdateResult.ProofGap(
                "avatar appearance response invalid: ${bodyDiagnostic(body)}",
            )
        return when (fields[SUCCESS_KEY]?.asBoolean()) {
            true -> AvatarAppearanceUpdateResult.Success
            false,
            null,
            -> AvatarAppearanceUpdateResult.ProofGap(invalidResponseDiagnostic(fields))
        }
    }

    private fun ProtocolHttpResponse.responseDiagnostic(): String =
        SafeDiagnosticRedaction.redact("$redactedSummary; ${bodyDiagnostic(body)}")

    private fun invalidResponseDiagnostic(fields: Map<String, LlsdValue>): String =
        "avatar appearance response invalid: ${fieldDiagnostic(fields)}; ${bodyDiagnostic(fields)}"

    private fun fieldDiagnostic(fields: Map<String, LlsdValue>): String = listOfNotNull(
        "success=${fields[SUCCESS_KEY]?.asBoolean()?.toString() ?: "<missing>"}",
        fields[ERROR_KEY]?.asString()?.takeIf(String::isNotBlank)?.let { "error=${SafeDiagnosticRedaction.redact(it)}" },
        fields[EXPECTED_KEY]?.asLong()?.let { "expected=$it" },
    ).joinToString("; ")

    private fun bodyDiagnostic(fields: Map<String, LlsdValue>): String =
        bodyDiagnosticText(fields.toDiagnosticBody())

    private fun bodyDiagnostic(body: ByteArray): String =
        bodyDiagnosticText(body.decodeToString())

    private fun bodyDiagnosticText(body: String): String =
        SafeDiagnosticRedaction.excerpt(body)
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"

    private fun Map<String, LlsdValue>.toDiagnosticBody(): String =
        entries.joinToString(
            prefix = "{",
            postfix = "}",
        ) { (key, value) -> "$key=${value.toDiagnosticValue()}" }

    private fun LlsdValue.toDiagnosticValue(): String = when (this) {
        is LlsdValue.ArrayValue -> values.joinToString(prefix = "[", postfix = "]") { it.toDiagnosticValue() }
        is LlsdValue.BooleanValue -> value.toString()
        is LlsdValue.MapValue -> values.toDiagnosticBody()
        is LlsdValue.ScalarValue -> SafeDiagnosticRedaction.redact(value)
        LlsdValue.Undefined -> "<undef>"
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
        const val EXPECTED_KEY = "expected"
    }
}
