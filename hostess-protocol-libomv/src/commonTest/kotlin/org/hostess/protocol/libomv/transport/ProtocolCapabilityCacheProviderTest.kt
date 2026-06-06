package org.hostess.protocol.libomv.transport

import org.hostess.core.domain.AccountLabel
import org.hostess.core.domain.HostessInstant
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.SessionId
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvSessionIdentity
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolCapabilityCacheProviderTest {
    @Test
    fun `cached url avoids seed request`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val session = hostessSession()
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
    fun `cold cache seeds once and stores returned urls`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val session = hostessSession()
        val clientSession = activeClientSession(session)
        val provider = provider(httpClient = httpClient, clientSession = clientSession)

        val eventResult = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )
        val inventoryResult = assertIs<CapabilityUrlResult.Ready>(
            provider.requireUrl(identity(), CapabilityName.FETCH_INVENTORY2),
        )

        assertEquals(eventUrl(), eventResult.url)
        assertEquals(fetchInventoryUrl(), inventoryResult.url)
        assertEquals(1, httpClient.requests.size)
    }

    @Test
    fun `missing requested name returns mapping gap at lookup`() {
        val provider = provider(
            httpClient = RecordingHttpClient(seedResponse(includeEventQueue = false)),
            clientSession = activeClientSession(hostessSession()),
        )

        val result = assertIs<CapabilityUrlResult.MappingGap>(
            provider.requireUrl(identity(), CapabilityName.EVENT_QUEUE_GET),
        )

        assertEquals("capability url absent: EventQueueGet", result.redactedMessage)
    }

    @Test
    fun `blank seed returns transport gap`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val provider = provider(
            httpClient = httpClient,
            clientSession = activeClientSession(hostessSession()),
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
            clientSession = activeClientSession(hostessSession()),
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
            clientSession = activeClientSession(hostessSession("different-session")),
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
        session: HostessSession,
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

    private fun hostessSession(id: String = SESSION_ID): HostessSession = HostessSession(
        sessionId = SessionId(id),
        accountLabel = AccountLabel("venue-proof"),
        startedAt = HostessInstant.EPOCH,
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

    private fun seedResponse(includeEventQueue: Boolean = true): ProtocolHttpResponse = response(
        buildString {
            append("<llsd><map>")
            if (includeEventQueue) {
                append("<key>EventQueueGet</key><uri>").append(eventUrl().value).append("</uri>")
            }
            append("<key>FetchInventory2</key><uri>").append(fetchInventoryUrl().value).append("</uri>")
            append("<key>FetchInventoryDescendents2</key><uri>").append(fetchDescendentsUrl().value).append("</uri>")
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

        fun fetchInventoryUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/fetch-inventory"))

        fun fetchDescendentsUrl(): CapabilityUrl = CapabilityUrl(secureUrl("caps.example", "/fetch-descendents"))

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
