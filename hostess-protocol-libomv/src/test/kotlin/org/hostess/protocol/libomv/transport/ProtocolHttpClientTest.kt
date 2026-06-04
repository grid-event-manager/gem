package org.hostess.protocol.libomv.transport

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProtocolHttpClientTest {
    @Test
    fun `request contract carries required transport fields`() {
        val body = ProtocolHttpBody.TextBody("<llsd />", "application/llsd+xml")
        val request = ProtocolHttpRequest(
            method = "POST",
            url = gridUrl("/login"),
            headers = mapOf("X-Proof" to "yes"),
            body = body,
            timeout = Duration.ofSeconds(12),
            redactionKeys = setOf("X-Proof"),
        )

        assertEquals("POST", request.method)
        assertEquals(gridUrl("/login"), request.url)
        assertEquals(mapOf("X-Proof" to "yes"), request.headers)
        assertSame(body, request.body)
        assertEquals(Duration.ofSeconds(12), request.timeout)
        assertEquals(setOf("X-Proof"), request.redactionKeys)
    }

    @Test
    fun `response contract carries status headers body and redacted summary`() {
        val response = ProtocolHttpResponse(
            statusCode = 201,
            headers = mapOf("Content-Type" to listOf("application/xml")),
            body = byteArrayOf(1, 2, 3),
            redactedSummary = "POST ${redactedGridUrl()} -> 201",
        )

        assertEquals(201, response.statusCode)
        assertEquals(mapOf("Content-Type" to listOf("application/xml")), response.headers)
        assertContentEquals(byteArrayOf(1, 2, 3), response.body)
        assertEquals("POST ${redactedGridUrl()} -> 201", response.redactedSummary)
    }

    private fun gridUrl(path: String): String = "https" + "://grid.example$path"

    private fun redactedGridUrl(): String = gridUrl("/<redacted>")
}
