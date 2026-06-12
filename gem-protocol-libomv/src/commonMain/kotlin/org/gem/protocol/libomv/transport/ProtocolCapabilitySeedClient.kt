package org.gem.protocol.libomv.transport

import org.gem.core.domain.GemDelay
import org.gem.core.services.SafeDiagnosticRedaction
import org.gem.protocol.libomv.llsd.LlsdXml
import org.gem.protocol.libomv.llsd.asString

internal class ProtocolCapabilitySeedClient(
    private val httpClient: ProtocolHttpClient,
) {
    fun seed(
        seedCapability: String,
        requested: Set<CapabilityName>,
    ): CapabilitySeedResult {
        if (seedCapability.isBlank()) {
            return CapabilitySeedResult.TransportGap("capability seed unavailable")
        }
        val response = when (val executed = execute(seedRequest(seedCapability, requested))) {
            is CapabilitySeedHttpResult.Failed ->
                return CapabilitySeedResult.TransportGap(executed.redactedMessage)
            is CapabilitySeedHttpResult.Success -> executed.response
        }
        if (response.statusCode !in 200..299) {
            return transportGap("http_status=${response.statusCode}; ${responseDiagnostic(response)}")
        }
        val fields = LlsdXml.parseMap(response.body)
            ?: return mappingGap("capability seed response invalid; ${bodyDiagnostic(response.body)}")
        val urls = linkedMapOf<CapabilityName, CapabilityUrl>()
        for (name in requested) {
            val value = fields[name.wireName] ?: continue
            val text = value.asString()?.takeIf(String::isNotBlank)
                ?: return mappingGap("capability url invalid: ${name.wireName}")
            urls[name] = CapabilityUrl(text)
        }
        return CapabilitySeedResult.Seeded(urls)
    }

    private fun execute(request: ProtocolHttpRequest): CapabilitySeedHttpResult = try {
        CapabilitySeedHttpResult.Success(httpClient.execute(request))
    } catch (ex: ProtocolHttpException) {
        CapabilitySeedHttpResult.Failed(
            transportMessage(ex.message ?: "protocol http request failed"),
        )
    }

    private fun seedRequest(seedCapability: String, requested: Set<CapabilityName>): ProtocolHttpRequest =
        ProtocolHttpRequest(
            method = "POST",
            url = seedCapability,
            headers = mapOf("Content-Type" to LLSD_XML),
            body = ProtocolHttpBody.TextBody(seedBody(requested), LLSD_XML),
            timeout = GemDelay.ofSeconds(30),
        )

    private fun seedBody(requested: Set<CapabilityName>): String = buildString {
        append("<llsd><array>")
        requested.forEach { name ->
            append("<string>").append(name.wireName).append("</string>")
        }
        append("</array></llsd>")
    }

    private fun transportGap(detail: String): CapabilitySeedResult.TransportGap =
        CapabilitySeedResult.TransportGap(transportMessage(detail))

    private fun mappingGap(detail: String): CapabilitySeedResult.MappingGap =
        CapabilitySeedResult.MappingGap("capability seed invalid: ${SafeDiagnosticRedaction.redact(detail)}")

    private fun transportMessage(detail: String): String =
        "capability seed unavailable: ${SafeDiagnosticRedaction.redact(detail)}"

    private fun responseDiagnostic(response: ProtocolHttpResponse): String =
        SafeDiagnosticRedaction.redact("${response.redactedSummary}; ${bodyDiagnostic(response.body)}")

    private fun bodyDiagnostic(body: ByteArray): String =
        SafeDiagnosticRedaction.excerpt(body.decodeToString())
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"

    private companion object {
        const val LLSD_XML = "application/llsd+xml"
    }
}

private sealed interface CapabilitySeedHttpResult {
    data class Success(val response: ProtocolHttpResponse) : CapabilitySeedHttpResult
    data class Failed(val redactedMessage: String) : CapabilitySeedHttpResult
}
