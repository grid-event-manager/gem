package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.LibomvMapping
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EventQueueGetClientTest {
    @Test
    fun `seed asks only for EventQueueGet`() {
        val httpClient = RecordingHttpClient(seedResponse())
        val client = EventQueueGetClient(httpClient)

        val result = assertIs<EventQueueGetResult.Ready>(client.seed(seedUrl()))

        assertEquals(eventUrl(), result.eventQueueUrl)
        val request = httpClient.requests.single()
        assertEquals("POST", request.method)
        assertEquals(seedUrl(), request.url)
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body).content
        assertEquals("<llsd><array><string>EventQueueGet</string></array></llsd>", body)
        assertFalse(body.contains("FetchInventoryDescendents"))
        assertFalse(body.contains("GroupProposalBallot"))
    }

    @Test
    fun `poll maps AgentGroupDataUpdate groups`() {
        val httpClient = RecordingHttpClient(groupEventResponse())
        val client = EventQueueGetClient(httpClient)

        val result = assertIs<EventQueueGetResult.AgentGroupDataUpdate>(
            client.pollAgentGroupDataUpdate(eventUrl()),
        )

        val group = result.groups.single()
        assertEquals(GROUP_ID, group.groupId)
        assertEquals("Music Room", group.displayName)
        assertEquals(LibomvMapping.SEND_NOTICES_POWER, group.powers)
        assertEquals(true, group.acceptsNotices)
        val request = httpClient.requests.single()
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body).content
        assertTrue(body.contains("<key>ack</key><undef/>"))
        assertTrue(body.contains("<key>done</key><boolean>false</boolean>"))
    }

    @Test
    fun `poll advances ack and times out without group event`() {
        val httpClient = RecordingHttpClient(
            mutableListOf(
                response(emptyEventsResponse(11)),
                response(emptyEventsResponse(12)),
            ),
        )
        val client = EventQueueGetClient(httpClient, maxPolls = 2)

        assertEquals(EventQueueGetResult.TimedOut, client.pollAgentGroupDataUpdate(eventUrl()))

        val secondBody = assertIs<ProtocolHttpBody.TextBody>(httpClient.requests[1].body).content
        assertTrue(secondBody.contains("<key>ack</key><integer>11</integer>"))
    }

    @Test
    fun `poll advances ack-only response and times out without group event`() {
        val httpClient = RecordingHttpClient(
            mutableListOf(
                response(ackOnlyResponse(21)),
                response(ackOnlyResponse(22)),
            ),
        )
        val client = EventQueueGetClient(httpClient, maxPolls = 2)

        assertEquals(EventQueueGetResult.TimedOut, client.pollAgentGroupDataUpdate(eventUrl()))

        val secondBody = assertIs<ProtocolHttpBody.TextBody>(httpClient.requests[1].body).content
        assertTrue(secondBody.contains("<key>ack</key><integer>21</integer>"))
    }

    @Test
    fun `transport failure returns transport gap`() {
        val client = EventQueueGetClient(ThrowingHttpClient())

        val result = assertIs<EventQueueGetResult.TransportGap>(client.seed(seedUrl()))

        assertContains(result.redactedMessage, "current groups transport unavailable")
        assertContains(result.redactedMessage, "redacted transport failure")
    }

    @Test
    fun `unrelated event before group event is ignored`() {
        val client = EventQueueGetClient(RecordingHttpClient(unrelatedThenGroupEventResponse()))

        val result = assertIs<EventQueueGetResult.AgentGroupDataUpdate>(
            client.pollAgentGroupDataUpdate(eventUrl()),
        )

        assertEquals("Music Room", result.groups.single().displayName)
    }

    @Test
    fun `unrelated event advances ack and times out without group event`() {
        val httpClient = RecordingHttpClient(
            mutableListOf(
                unrelatedEventResponse(),
                response(emptyEventsResponse(9)),
            ),
        )
        val client = EventQueueGetClient(httpClient, maxPolls = 2)

        assertEquals(EventQueueGetResult.TimedOut, client.pollAgentGroupDataUpdate(eventUrl()))

        val secondBody = assertIs<ProtocolHttpBody.TextBody>(httpClient.requests[1].body).content
        assertTrue(secondBody.contains("<key>ack</key><integer>8</integer>"))
    }

    @Test
    fun `malformed group event returns mapping gap`() {
        val client = EventQueueGetClient(RecordingHttpClient(malformedGroupEventResponse()))

        val result = assertIs<EventQueueGetResult.MappingGap>(client.pollAgentGroupDataUpdate(eventUrl()))

        assertContains(result.redactedMessage, "current groups event invalid")
        assertContains(result.redactedMessage, "agent group event agent data invalid")
    }

    @Test
    fun `non success response returns redacted transport detail`() {
        val client = EventQueueGetClient(
            RecordingHttpClient(
                response(
                    body = "<llsd>${secureUrl("secret.example", "/seed")}</llsd>".encodeToByteArray(),
                    statusCode = 503,
                    redactedSummary = "POST ${secureUrl("secret.example", "/seed")} -> 503",
                ),
            ),
        )

        val result = assertIs<EventQueueGetResult.TransportGap>(client.seed(seedUrl()))

        assertContains(result.redactedMessage, "http_status=503")
        assertContains(result.redactedMessage, "[redacted-url]")
        assertFalse(result.redactedMessage.contains("secret.example"))
    }

    private class RecordingHttpClient(
        private val responses: MutableList<ProtocolHttpResponse>,
    ) : ProtocolHttpClient {
        constructor(response: ProtocolHttpResponse) : this(mutableListOf(response))

        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            return responses.removeAt(0)
        }
    }

    private class ThrowingHttpClient : ProtocolHttpClient {
        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            throw ProtocolHttpException("redacted transport failure")
        }
    }

    private fun seedResponse(): ProtocolHttpResponse = response(
        """
        <llsd>
          <map>
            <key>EventQueueGet</key><uri>${eventUrl()}</uri>
          </map>
        </llsd>
        """.trimIndent().encodeToByteArray(),
    )

    private fun groupEventResponse(): ProtocolHttpResponse = response(
        """
        <llsd>
          <map>
            <key>events</key>
            <array>
              <map>
                <key>message</key><string>AgentGroupDataUpdate</string>
                <key>body</key>
                <map>
                  <key>AgentData</key>
                  <array>
                    <map>
                      <key>AgentID</key><uuid>$AGENT_ID</uuid>
                    </map>
                  </array>
                  <key>GroupData</key>
                  <array>
                    <map>
                      <key>AcceptNotices</key><boolean>true</boolean>
                      <key>Contribution</key><integer>0</integer>
                      <key>GroupID</key><uuid>$GROUP_ID</uuid>
                      <key>GroupInsigniaID</key><uuid>00000000-0000-0000-0000-000000000000</uuid>
                      <key>GroupName</key><string>Music Room</string>
                      <key>GroupTitle</key><string>Host</string>
                      <key>GroupPowers</key><integer>${LibomvMapping.SEND_NOTICES_POWER}</integer>
                    </map>
                  </array>
                  <key>NewGroupData</key>
                  <array>
                    <map>
                      <key>ListInProfile</key><boolean>false</boolean>
                    </map>
                  </array>
                </map>
              </map>
            </array>
            <key>id</key><integer>7</integer>
          </map>
        </llsd>
        """.trimIndent().encodeToByteArray(),
    )

    private fun unrelatedThenGroupEventResponse(): ProtocolHttpResponse = response(
        """
        <llsd>
          <map>
            <key>events</key>
            <array>
              <map>
                <key>message</key><string>TeleportFinish</string>
                <key>body</key><map></map>
              </map>
              <map>
                <key>message</key><string>AgentGroupDataUpdate</string>
                <key>body</key>
                <map>
                  <key>AgentData</key>
                  <array>
                    <map>
                      <key>AgentID</key><uuid>$AGENT_ID</uuid>
                    </map>
                  </array>
                  <key>GroupData</key>
                  <array>
                    <map>
                      <key>AcceptNotices</key><boolean>true</boolean>
                      <key>Contribution</key><integer>0</integer>
                      <key>GroupID</key><uuid>$GROUP_ID</uuid>
                      <key>GroupName</key><string>Music Room</string>
                      <key>GroupTitle</key><string>Host</string>
                      <key>GroupPowers</key><integer>${LibomvMapping.SEND_NOTICES_POWER}</integer>
                    </map>
                  </array>
                </map>
              </map>
            </array>
            <key>id</key><integer>8</integer>
          </map>
        </llsd>
        """.trimIndent().encodeToByteArray(),
    )

    private fun emptyEventsResponse(id: Long): ByteArray = """
        <llsd>
          <map>
            <key>events</key><array></array>
            <key>id</key><integer>$id</integer>
          </map>
        </llsd>
    """.trimIndent().encodeToByteArray()

    private fun ackOnlyResponse(id: Long): ByteArray = """
        <llsd>
          <map>
            <key>ack</key><integer>$id</integer>
            <key>done</key><boolean>false</boolean>
          </map>
        </llsd>
    """.trimIndent().encodeToByteArray()

    private fun unrelatedEventResponse(): ProtocolHttpResponse = response(
        """
        <llsd>
          <map>
            <key>events</key>
            <array>
              <map>
                <key>message</key><string>TeleportFinish</string>
                <key>body</key><map></map>
              </map>
            </array>
            <key>id</key><integer>8</integer>
          </map>
        </llsd>
        """.trimIndent().encodeToByteArray(),
    )

    private fun malformedGroupEventResponse(): ProtocolHttpResponse = response(
        """
        <llsd>
          <map>
            <key>events</key>
            <array>
              <map>
                <key>message</key><string>AgentGroupDataUpdate</string>
                <key>body</key>
                <map>
                  <key>GroupData</key>
                  <array>
                    <map>
                      <key>GroupID</key><uuid>$GROUP_ID</uuid>
                      <key>GroupName</key><string>Music Room</string>
                    </map>
                  </array>
                </map>
              </map>
            </array>
            <key>id</key><integer>9</integer>
          </map>
        </llsd>
        """.trimIndent().encodeToByteArray(),
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
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val GROUP_ID = "33333333-3333-3333-3333-333333333333"

        fun seedUrl(): String = secureUrl("caps.example", "/seed")

        fun eventUrl(): String = secureUrl("caps.example", "/event")

        fun secureUrl(host: String, path: String): String = "https" + "://$host$path"
    }
}
