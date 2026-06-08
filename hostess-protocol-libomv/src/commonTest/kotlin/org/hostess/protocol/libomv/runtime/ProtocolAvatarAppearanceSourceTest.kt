package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.CapabilityUrl
import org.hostess.protocol.libomv.transport.ProtocolHttpBody
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolHttpException
import org.hostess.protocol.libomv.transport.ProtocolHttpRequest
import org.hostess.protocol.libomv.transport.ProtocolHttpResponse

class ProtocolAvatarAppearanceSourceTest {
    @Test
    fun `posts update avatar appearance llsd body with cof version only`() {
        val httpClient = RecordingHttpClient { successResponse() }
        val source = ProtocolAvatarAppearanceSource(httpClient)

        val result = source.updateServerAppearance(identity(), 17, capabilityUrl())

        assertEquals(AvatarAppearanceUpdateResult.Success, result)
        val request = httpClient.requests.single()
        assertEquals("POST", request.method)
        assertEquals(capabilityUrl().value, request.url)
        assertEquals("application/llsd+xml", request.headers["Content-Type"])
        val body = assertIs<ProtocolHttpBody.TextBody>(request.body).content
        assertEquals("application/llsd+xml", assertIs<ProtocolHttpBody.TextBody>(request.body).contentType)
        assertContains(body, "<key>cof_version</key><integer>17</integer>")
        assertFalse(body.contains(AGENT_ID))
        assertFalse(body.contains(SESSION_ID))
        assertFalse(body.contains("seed"))
        assertEquals(1, body.split("<key>").size - 1)
    }

    @Test
    fun `http non success and protocol exception are transport gaps`() {
        val statusFailure = ProtocolAvatarAppearanceSource(
            RecordingHttpClient { response("<llsd><map /></llsd>", statusCode = 503) },
        ).updateServerAppearance(identity(), 17, capabilityUrl())
        val exceptionFailure = ProtocolAvatarAppearanceSource(
            RecordingHttpClient { throw ProtocolHttpException("timeout from https://caps.example/appearance") },
        ).updateServerAppearance(identity(), 17, capabilityUrl())

        assertEquals(
            "avatar appearance transport unavailable",
            assertIs<AvatarAppearanceUpdateResult.TransportGap>(statusFailure).redactedMessage,
        )
        assertEquals(
            "avatar appearance transport unavailable",
            assertIs<AvatarAppearanceUpdateResult.TransportGap>(exceptionFailure).redactedMessage,
        )
    }

    @Test
    fun `malformed missing false or error responses are proof gaps without body leakage`() {
        val cases = listOf(
            "",
            "<llsd><map>",
            "<llsd><map><key>other</key><boolean>true</boolean></map></llsd>",
            "<llsd><map><key>success</key><boolean>false</boolean></map></llsd>",
            "<llsd><map><key>error</key><string>raw grid body</string></map></llsd>",
        )

        cases.forEach { body ->
            val result = ProtocolAvatarAppearanceSource(
                RecordingHttpClient { response(body) },
            ).updateServerAppearance(identity(), 17, capabilityUrl())

            val message = assertIs<AvatarAppearanceUpdateResult.ProofGap>(result).redactedMessage
            assertEquals("avatar appearance response invalid", message)
            if (body.isNotEmpty()) {
                assertFalse(message.contains(body))
            }
            assertFalse(message.contains("raw grid body"))
            assertFalse(message.contains(capabilityUrl().value))
            assertFalse(message.contains("17"))
            assertFalse(message.contains(AGENT_ID))
        }
    }

    private class RecordingHttpClient(
        private val responseForRequest: (ProtocolHttpRequest) -> ProtocolHttpResponse,
    ) : ProtocolHttpClient {
        val requests = mutableListOf<ProtocolHttpRequest>()

        override fun execute(request: ProtocolHttpRequest): ProtocolHttpResponse {
            requests += request
            return responseForRequest(request)
        }
    }

    private fun successResponse(): ProtocolHttpResponse =
        response("<llsd><map><key>success</key><boolean>true</boolean></map></llsd>")

    private fun response(
        body: String,
        statusCode: Int = 200,
    ): ProtocolHttpResponse = ProtocolHttpResponse(
        statusCode = statusCode,
        headers = emptyMap(),
        body = body.encodeToByteArray(),
        redactedSummary = "POST <redacted> -> $statusCode",
    )

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = AGENT_ID,
        sessionId = SESSION_ID,
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )

    private fun capabilityUrl(): CapabilityUrl = CapabilityUrl("https://caps.example/appearance")

    private companion object {
        const val AGENT_ID = "11111111-1111-1111-1111-111111111111"
        const val SESSION_ID = "22222222-2222-2222-2222-222222222222"
    }
}
