package org.hostess.protocol.libomv.transport

import org.hostess.core.domain.HostessDelay

internal data class ProtocolHttpRequestPolicyResult(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ProtocolHttpBody,
    val timeout: HostessDelay,
    val redactionKeys: Set<String>,
    val redactedTarget: String,
) {
    fun redactedSummary(statusCode: Int, responseHeaderNames: Collection<String>): String =
        "$method $redactedTarget -> $statusCode; requestHeaders=${headers.redactedHeaderSummary(redactionKeys)}; " +
            "responseHeaders=${responseHeaderNames.sorted()}"
}

internal object ProtocolHttpRequestPolicy {
    fun normalize(request: ProtocolHttpRequest): ProtocolHttpRequestPolicyResult {
        val method = request.method.trim().uppercase()
        if (method.isBlank()) {
            throw ProtocolHttpException("Invalid protocol HTTP request: method is blank")
        }
        if (request.url.isBlank()) {
            throw ProtocolHttpException("Invalid protocol HTTP request: URL is blank")
        }
        val endpoint = ProtocolHttpEndpoint.parse(request.url)
            ?: throw ProtocolHttpException("Invalid protocol HTTP request: URL is malformed")
        when (endpoint.scheme) {
            "https" -> Unit
            "http" -> throw ProtocolHttpException("Invalid protocol HTTP request: HTTP is not supported")
            else -> throw ProtocolHttpException("Invalid protocol HTTP request: unsupported URL scheme")
        }
        if (request.body !is ProtocolHttpBody.NoBody && method in METHODS_WITHOUT_BODY) {
            throw ProtocolHttpException("Invalid protocol HTTP request: $method cannot carry a request body")
        }
        if (!request.timeout.isPositive || request.timeout.milliseconds > MAX_TIMEOUT_MILLISECONDS) {
            throw ProtocolHttpException("Invalid protocol HTTP request: timeout is out of range")
        }

        return ProtocolHttpRequestPolicyResult(
            method = method,
            url = request.url,
            headers = request.headers,
            body = request.body,
            timeout = request.timeout,
            redactionKeys = request.redactionKeys,
            redactedTarget = endpoint.redactedTarget(),
        )
    }

    private val METHODS_WITHOUT_BODY = setOf("GET", "HEAD")
    private const val MAX_TIMEOUT_MILLISECONDS = Int.MAX_VALUE.toLong()
}

private data class ProtocolHttpEndpoint(
    val scheme: String,
    val host: String,
    val port: Int?,
) {
    fun redactedTarget(): String {
        val portText = port?.let { ":$it" }.orEmpty()
        return "$scheme://$host$portText/<redacted>"
    }

    companion object {
        fun parse(url: String): ProtocolHttpEndpoint? {
            if (url.any { it.isWhitespace() }) {
                return null
            }
            val schemeMarker = url.indexOf("://")
            if (schemeMarker <= 0) {
                return null
            }
            val scheme = url.substring(0, schemeMarker).lowercase()
            if (scheme.any { it !in 'a'..'z' && it !in '0'..'9' && it != '+' && it != '-' && it != '.' }) {
                return null
            }
            val remainder = url.substring(schemeMarker + 3)
            val authorityEnd = remainder.indexOfAny(charArrayOf('/', '?', '#')).takeIf { it >= 0 } ?: remainder.length
            val authority = remainder.substring(0, authorityEnd).substringAfterLast('@')
            if (authority.isBlank()) {
                return null
            }
            val hostAndPort = parseHostAndPort(authority) ?: return null
            return ProtocolHttpEndpoint(
                scheme = scheme,
                host = hostAndPort.host,
                port = hostAndPort.port,
            )
        }

        private fun parseHostAndPort(authority: String): HostAndPort? {
            if (authority.startsWith("[")) {
                val close = authority.indexOf(']')
                if (close <= 1) {
                    return null
                }
                val host = authority.substring(0, close + 1)
                val suffix = authority.substring(close + 1)
                val port = if (suffix.isEmpty()) null else suffix.parsePortSuffix() ?: return null
                return HostAndPort(host, port)
            }
            if (authority.count { it == ':' } > 1) {
                return null
            }
            val separator = authority.lastIndexOf(':')
            if (separator == -1) {
                return HostAndPort(authority.takeIf(String::isNotBlank) ?: return null, null)
            }
            val host = authority.substring(0, separator).takeIf(String::isNotBlank) ?: return null
            val port = authority.substring(separator).parsePortSuffix() ?: return null
            return HostAndPort(host, port)
        }

        private fun String.parsePortSuffix(): Int? {
            if (isEmpty()) {
                return null
            }
            if (this[0] != ':') {
                return null
            }
            val text = drop(1).takeIf(String::isNotBlank) ?: return null
            return text.toIntOrNull()?.takeIf { it in 1..MAX_PORT }
        }

        private const val MAX_PORT = 65535
    }
}

private data class HostAndPort(
    val host: String,
    val port: Int?,
)

private fun Map<String, String>.redactedHeaderSummary(redactionKeys: Set<String>): List<String> {
    val redacted = redactionKeys.map { it.lowercase() }.toSet()
    return entries
        .sortedBy { it.key.lowercase() }
        .map { (name, value) ->
            val display = if (name.lowercase() in redacted) "<redacted>" else "<present:${value.length}>"
            "$name=$display"
        }
}
