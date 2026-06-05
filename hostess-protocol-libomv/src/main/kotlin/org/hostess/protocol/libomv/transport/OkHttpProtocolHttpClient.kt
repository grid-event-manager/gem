package org.hostess.protocol.libomv.transport

import java.io.IOException
import java.util.concurrent.TimeUnit
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
        val normalized = ProtocolHttpRequestPolicy.normalize(request)
        val okRequest = try {
            normalized.toOkHttpRequest()
        } catch (ex: IllegalArgumentException) {
            throw ProtocolHttpException("Invalid protocol HTTP request: request mapping failed")
        }
        val client = baseClient.newBuilder()
            .callTimeout(normalized.timeout.milliseconds, TimeUnit.MILLISECONDS)
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
                    redactedSummary = normalized.redactedSummary(response.code, response.headers.toMultimap().keys),
                )
            }
        } catch (ex: IOException) {
            throw ProtocolHttpException("Protocol HTTP request failed: ${normalized.redactedTarget}")
        }
    }

    private fun ProtocolHttpRequestPolicyResult.toOkHttpRequest(): Request {
        val builder = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
        builder.method(method, body.toOkHttpBody(method))
        return builder.build()
    }

    companion object {
        private val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")

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
    }
}
