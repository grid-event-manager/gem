package org.hostess.protocol.libomv.transport

import java.io.IOException
import java.net.URI
import java.time.Duration
import java.util.Locale
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpProtocolHttpClient internal constructor(
    private val baseClient: OkHttpClient,
) : ProtocolHttpClient {
    constructor() : this(OkHttpClient())

    override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
        val normalized = normalize(request)
        val okRequest = try {
            normalized.toOkHttpRequest()
        } catch (ex: IllegalArgumentException) {
            throw ProtocolHttpException("Invalid protocol HTTP request: request mapping failed")
        }
        val client = baseClient.newBuilder()
            .callTimeout(normalized.timeout)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return try {
            client.newCall(okRequest).execute().use { response ->
                val body = response.body.bytes()
                ProtocolHttpResponse(
                    statusCode = response.code,
                    headers = response.headers.toMultimap(),
                    body = body,
                    redactedSummary = normalized.redactedSummary(response.code, response.headers),
                )
            }
        } catch (ex: IOException) {
            throw ProtocolHttpException("Protocol HTTP request failed: ${normalized.redactedTarget()}")
        }
    }

    private fun normalize(request: ProtocolHttpRequest): NormalizedRequest {
        val method = request.method.trim().uppercase(Locale.ROOT)
        if (method.isBlank()) {
            throw ProtocolHttpException("Invalid protocol HTTP request: method is blank")
        }
        if (request.url.isBlank()) {
            throw ProtocolHttpException("Invalid protocol HTTP request: URL is blank")
        }
        if (request.timeout.isNegative) {
            throw ProtocolHttpException("Invalid protocol HTTP request: timeout is negative")
        }

        val uri = try {
            URI(request.url)
        } catch (ex: IllegalArgumentException) {
            throw ProtocolHttpException("Invalid protocol HTTP request: URL is malformed")
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
            ?: throw ProtocolHttpException("Invalid protocol HTTP request: URL scheme is absent")
        when (scheme) {
            "https" -> Unit
            "http" -> if (!uri.isLocalTestServer()) {
                throw ProtocolHttpException("Invalid protocol HTTP request: HTTP is allowed only for local test servers")
            }
            else -> throw ProtocolHttpException("Invalid protocol HTTP request: unsupported URL scheme")
        }
        if (request.body !is ProtocolHttpBody.NoBody && method in METHODS_WITHOUT_BODY) {
            throw ProtocolHttpException("Invalid protocol HTTP request: $method cannot carry a request body")
        }

        return NormalizedRequest(
            method = method,
            uri = uri,
            headers = request.headers,
            body = request.body,
            timeout = request.timeout,
            redactionKeys = request.redactionKeys,
        )
    }

    private data class NormalizedRequest(
        val method: String,
        val uri: URI,
        val headers: Map<String, String>,
        val body: ProtocolHttpBody,
        val timeout: Duration,
        val redactionKeys: Set<String>,
    ) {
        fun toOkHttpRequest(): Request {
            val builder = Request.Builder()
                .url(uri.toString())
                .headers(headers.toHeaders())
            builder.method(method, body.toOkHttpBody(method))
            return builder.build()
        }

        fun redactedSummary(statusCode: Int, responseHeaders: Headers): String =
            "$method ${redactedTarget()} -> $statusCode; requestHeaders=${headers.redactedHeaderSummary(redactionKeys)}; " +
                "responseHeaders=${responseHeaders.toMultimap().keys.sorted()}"

        fun redactedTarget(): String {
            val host = uri.host ?: "host-redacted"
            val port = if (uri.port >= 0) ":${uri.port}" else ""
            return "${uri.scheme}://$host$port/<redacted>"
        }
    }

    companion object {
        private val METHODS_WITHOUT_BODY = setOf("GET", "HEAD")
        private val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")

        private fun URI.isLocalTestServer(): Boolean {
            val host = host?.lowercase(Locale.ROOT) ?: return false
            return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]"
        }

        private fun Map<String, String>.toHeaders(): Headers =
            Headers.Builder().also { builder ->
                forEach { (name, value) -> builder.add(name, value) }
            }.build()

        private fun ProtocolHttpBody.toOkHttpBody(method: String): RequestBody? = when (this) {
            ProtocolHttpBody.NoBody -> if (method in METHODS_REQUIRING_BODY) {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            }
            is ProtocolHttpBody.TextBody -> content.encodeToByteArray().toRequestBody(contentType.toMediaType())
            is ProtocolHttpBody.BinaryUploadBody -> bytes.toRequestBody(contentType.toMediaType())
        }

        private fun Map<String, String>.redactedHeaderSummary(redactionKeys: Set<String>): List<String> {
            val redacted = redactionKeys.map { it.lowercase(Locale.ROOT) }.toSet()
            return entries
                .sortedBy { it.key.lowercase(Locale.ROOT) }
                .map { (name, value) ->
                    val display = if (name.lowercase(Locale.ROOT) in redacted) "<redacted>" else "<present:${value.length}>"
                    "$name=$display"
                }
        }
    }
}
