package org.hostess.protocol.libomv.transport

import org.hostess.core.domain.HostessDelay

data class ProtocolHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ProtocolHttpBody = ProtocolHttpBody.NoBody,
    val timeout: HostessDelay = HostessDelay.ofSeconds(30),
    val redactionKeys: Set<String> = emptySet(),
)

sealed interface ProtocolHttpBody {
    data object NoBody : ProtocolHttpBody

    data class TextBody(
        val content: String,
        val contentType: String = "application/xml; charset=utf-8",
    ) : ProtocolHttpBody
}

data class ProtocolHttpResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
    val redactedSummary: String,
)

interface ProtocolHttpClient {
    fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse
}

class ProtocolHttpException(message: String) : RuntimeException(message)
