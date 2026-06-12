package org.gem.protocol.libomv.transport

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ProtocolCapabilitySeedClientTest {
    @Test
    fun `seed posts production capability names and maps typed urls`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val client = ProtocolCapabilitySeedClient(httpClient)

        val result = assertIs<CapabilitySeedResult.Seeded>(
            client.seed(seedUrl(), ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS),
        )

        assertEquals(eventUrl(), result.urls[CapabilityName.EVENT_QUEUE_GET])
        assertEquals(fetchDescendentsUrl(), result.urls[CapabilityName.FETCH_INVENTORY_DESCENDENTS2])
        assertEquals(updateAvatarAppearanceUrl(), result.urls[CapabilityName.UPDATE_AVATAR_APPEARANCE])
        val request = httpClient.requests.single()
        assertEquals("POST", request.method)
        assertEquals(seedUrl(), request.url)
        assertEquals("application/llsd+xml", request.headers["Content-Type"])
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body).content
        assertEquals(
            "<llsd><array>" +
                "<string>EventQueueGet</string>" +
                "<string>FetchInventoryDescendents2</string>" +
                "<string>UpdateAvatarAppearance</string>" +
                "</array></llsd>",
            body,
        )
        assertFalse(body.contains("GroupProposalBallot"))
    }

    @Test
    fun `seed ignores unknown returned names`() {
        val client = ProtocolCapabilitySeedClient(RecordingHttpClient(seedResponse(includeUnknown = true)))

        val result = assertIs<CapabilitySeedResult.Seeded>(
            client.seed(seedUrl(), setOf(CapabilityName.EVENT_QUEUE_GET)),
        )

        assertEquals(setOf(CapabilityName.EVENT_QUEUE_GET), result.urls.keys)
    }

    @Test
    fun `blank seed returns transport gap without http call`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val client = ProtocolCapabilitySeedClient(httpClient)

        val result = assertIs<CapabilitySeedResult.TransportGap>(
            client.seed("", ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS),
        )

        assertEquals("capability seed unavailable", result.redactedMessage)
        assertEquals(emptyList(), httpClient.requests)
    }

    @Test
    fun `malformed seed response returns mapping gap`() {
        val client = ProtocolCapabilitySeedClient(
            RecordingHttpClient(response("<llsd><map>".encodeToByteArray())),
        )

        val result = assertIs<CapabilitySeedResult.MappingGap>(
            client.seed(seedUrl(), ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS),
        )

        assertContains(result.redactedMessage, "capability seed invalid")
        assertContains(result.redactedMessage, "capability seed response invalid")
    }

    @Test
    fun `transport failure returns redacted transport gap`() {
        val client = ProtocolCapabilitySeedClient(ThrowingHttpClient())

        val result = assertIs<CapabilitySeedResult.TransportGap>(
            client.seed(seedUrl(), ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS),
        )

        assertContains(result.redactedMessage, "capability seed unavailable")
        assertContains(result.redactedMessage, "redacted transport failure")
    }

    @Test
    fun `non success seed response redacts url material`() {
        val client = ProtocolCapabilitySeedClient(
            RecordingHttpClient(
                response(
                    body = "<llsd>${secureUrl("secret.example", "/seed")}</llsd>".encodeToByteArray(),
                    statusCode = 503,
                    redactedSummary = "POST ${secureUrl("secret.example", "/seed")} -> 503",
                ),
            ),
        )

        val result = assertIs<CapabilitySeedResult.TransportGap>(
            client.seed(seedUrl(), ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS),
        )

        assertContains(result.redactedMessage, "http_status=503")
        assertContains(result.redactedMessage, "[redacted-url]")
        assertFalse(result.redactedMessage.contains("secret.example"))
    }

    private class RecordingHttpClient(
        private val response: ProtocolHttpResponse,
    ) : ProtocolHttpClient {
        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            return response
        }
    }

    private class ThrowingHttpClient : ProtocolHttpClient {
        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            throw ProtocolHttpException("redacted transport failure")
        }
    }

    private fun seedResponse(includeUnknown: Boolean = false): ProtocolHttpResponse = response(
        buildString {
            append("<llsd><map>")
            append("<key>EventQueueGet</key><uri>").append(eventUrl().value).append("</uri>")
            append("<key>FetchInventoryDescendents2</key><uri>").append(fetchDescendentsUrl().value).append("</uri>")
            append("<key>UpdateAvatarAppearance</key><uri>").append(updateAvatarAppearanceUrl().value).append("</uri>")
            if (includeUnknown) {
                append("<key>GroupProposalBallot</key><uri>").append(secureUrl("caps.example", "/ballot")).append("</uri>")
            }
            append("</map></llsd>")
        }.encodeToByteArray(),
    )

    private fun response(
        body: ByteArray,
        statusCode: Int = 200,
        redactedSummary: String = "POST <redacted> -> $statusCode",
    ): ProtocolHttpResponse = ProtocolHttpResponse(
        statusCode = statusCode,
        headers = emptyMap(),
        body = body,
        redactedSummary = redactedSummary,
    )

    private companion object {
        fun seedUrl(): String = secureUrl("caps.example", "/seed")

        fun eventUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/event"))

        fun fetchDescendentsUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/fetch-descendents"))

        fun updateAvatarAppearanceUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/appearance"))

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
