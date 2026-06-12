package org.gem.protocol.libomv.transport

import org.gem.core.domain.AccountLabel
import org.gem.core.domain.GemInstant
import org.gem.core.domain.GemSession
import org.gem.core.domain.SessionId
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolCapabilityCacheProviderTest {
    @Test
    fun `production capability requests include avatar appearance capability`() {
        assertEquals(
            setOf(
                CapabilityName.EVENT_QUEUE_GET,
                CapabilityName.FETCH_INVENTORY_DESCENDENTS2,
                CapabilityName.UPDATE_AVATAR_APPEARANCE,
            ),
            ProtocolCapabilityCacheProvider.PRODUCTION_CAPABILITY_REQUESTS,
        )
    }

    @Test
    fun `cached url avoids seed request`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val session = gemSession()
        val provider = provider(
            httpClient = httpClient,
            clientSession = activeClientSession(
                session = session,
                capabilityCache = CapabilityCache(
                    mapOf(CapabilityName.EVENT_QUEUE_GET to eventUrl()),
                ),
            ),
        )

        val result = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )

        assertEquals(eventUrl(), result.url)
        assertEquals(emptyList(), httpClient.requests)
    }

    @Test
    fun `cached avatar appearance url avoids seed request`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val session = gemSession()
        val provider = provider(
            httpClient = httpClient,
            clientSession = activeClientSession(
                session = session,
                capabilityCache = CapabilityCache(
                    mapOf(CapabilityName.UPDATE_AVATAR_APPEARANCE to updateAvatarAppearanceUrl()),
                ),
            ),
        )

        val result = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.UPDATE_AVATAR_APPEARANCE),
        )

        assertEquals(updateAvatarAppearanceUrl(), result.url)
        assertEquals(emptyList(), httpClient.requests)
    }

    @Test
    fun `cold cache seeds once and stores returned urls`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val session = gemSession()
        val clientSession = activeClientSession(session)
        val provider = provider(httpClient = httpClient, clientSession = clientSession)

        val eventResult = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )
        val inventoryResult = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.FETCH_INVENTORY_DESCENDENTS2),
        )

        assertEquals(eventUrl(), eventResult.url)
        assertEquals(fetchDescendentsUrl(), inventoryResult.url)
        assertEquals(1, httpClient.requests.size)
    }

    @Test
    fun `missing requested name returns mapping gap at lookup`() {
        val provider = provider(
            httpClient = RecordingHttpClient(seedResponse(includeEventQueue = false)),
            clientSession = activeClientSession(gemSession()),
        )

        val result = assertIs<CapabilityUrlResult.MappingGap>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )

        assertEquals("capability url absent: EventQueueGet", result.redactedMessage)
    }

    @Test
    fun `missing avatar appearance capability returns mapping gap at lookup`() {
        val provider = provider(
            httpClient = RecordingHttpClient(seedResponse(includeUpdateAvatarAppearance = false)),
            clientSession = activeClientSession(gemSession()),
        )

        val result = assertIs<CapabilityUrlResult.MappingGap>(
            provider.requireUrl(identity(), CapabilityName.UPDATE_AVATAR_APPEARANCE),
        )

        assertEquals("capability url absent: UpdateAvatarAppearance", result.redactedMessage)
    }

    @Test
    fun `blank seed returns transport gap`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val provider = provider(
            httpClient = httpClient,
            clientSession = activeClientSession(gemSession()),
        )

        val result = assertIs<CapabilityUrlResult.TransportGap>(
            provider.requireUrl(identity(seedCapability = ""), CapabilityName.EVENT_QUEUE_GET),
        )

        assertEquals("capability seed unavailable", result.redactedMessage)
        assertEquals(emptyList(), httpClient.requests)
    }

    @Test
    fun `malformed seed response returns mapping gap`() {
        val provider = provider(
            httpClient = RecordingHttpClient(response("<llsd><map>".encodeToByteArray())),
            clientSession = activeClientSession(gemSession()),
        )

        val result = assertIs<CapabilityUrlResult.MappingGap>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )

        assertContains(result.redactedMessage, "capability seed invalid")
    }

    @Test
    fun `session mismatch fails without seed request`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val provider = provider(
            httpClient = httpClient,
            clientSession = activeClientSession(gemSession("different-session")),
        )

        val result = assertIs<CapabilityUrlResult.TransportGap>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )

        assertEquals("capability cache unavailable", result.redactedMessage)
        assertEquals(emptyList(), httpClient.requests)
    }

    private fun provider(
        httpClient: RecordingHttpClient,
        clientSession: LibomvClientSession,
    ): ProtocolCapabilityCacheProvider = ProtocolCapabilityCacheProvider(
        clientSession = clientSession,
        seedClient = ProtocolCapabilitySeedClient(httpClient),
    )

    private class RecordingHttpClient(
        private val response: ProtocolHttpResponse,
    ) : ProtocolHttpClient {
        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            return response
        }
    }

    private fun activeClientSession(
        session: GemSession,
        capabilityCache: CapabilityCache = CapabilityCache.empty(),
    ): LibomvClientSession = LibomvClientSession.active(
        session = session,
        agentId = "11111111-1111-1111-1111-111111111111",
        seedCapability = seedUrl(),
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
        capabilityCache = capabilityCache,
    )

    private fun gemSession(id: String = SESSION_ID): GemSession = GemSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = GemInstant.EPOCH,
        isActive = true,
    )

    private fun identity(seedCapability: String = seedUrl()): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = "11111111-1111-1111-1111-111111111111",
        sessionId = SESSION_ID,
        seedCapability = seedCapability,
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )

    private fun seedResponse(
        includeEventQueue: Boolean = true,
        includeUpdateAvatarAppearance: Boolean = true,
    ): ProtocolHttpResponse = response(
        buildString {
            append("<llsd><map>")
            if (includeEventQueue) {
                append("<key>EventQueueGet</key><uri>").append(eventUrl().value).append("</uri>")
            }
            append("<key>FetchInventoryDescendents2</key><uri>").append(fetchDescendentsUrl().value).append("</uri>")
            if (includeUpdateAvatarAppearance) {
                append("<key>UpdateAvatarAppearance</key><uri>").append(updateAvatarAppearanceUrl().value).append("</uri>")
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
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"

        fun seedUrl(): String = secureUrl("caps.example", "/seed")

        fun eventUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/event"))

        fun fetchDescendentsUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/fetch-descendents"))

        fun updateAvatarAppearanceUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/appearance"))

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
