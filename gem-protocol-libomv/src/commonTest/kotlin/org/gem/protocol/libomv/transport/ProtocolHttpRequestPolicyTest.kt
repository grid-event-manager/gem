package org.gem.protocol.libomv.transport

import org.gem.core.domain.GemDelay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolHttpRequestPolicyTest {
    @Test
    fun `normalizes method and redacts request summary`() {
        val result = ProtocolHttpRequestPolicy.normalize(
            ProtocolHttpRequest(
                method = "post",
                url = "${gridUrl("/login")}?token=secret",
                headers = mapOf("Authorization" to "secret-token", "X-Proof" to "abc"),
                body = ProtocolHttpBody.TextBody("secret-body"),
                timeout = GemDelay.ofSeconds(5),
                redactionKeys = setOf("Authorization"),
            ),
        )

        assertEquals("POST", result.method)
        assertEquals(GemDelay.ofSeconds(5), result.timeout)
        assertEquals(redactedGridUrl(), result.redactedTarget)
        val summary = result.redactedSummary(202, setOf("Set-Cookie", "X-Reply"))
        assertTrue(summary.contains("Authorization=<redacted>"))
        assertTrue(summary.contains("X-Proof=<present:3>"))
        assertFalse(summary.contains("secret-token"))
        assertFalse(summary.contains("secret-body"))
        assertFalse(summary.contains("/login"))
    }

    @Test
    fun `rejects all plain HTTP including local endpoints`() {
        listOf(
            "http://127.0.0.1:8123/redirect",
            "http://localhost:8123/redirect",
            "http://[::1]:8123/redirect",
            "http://grid.example/login",
        ).forEach { url ->
            val failure = assertFailsWith<ProtocolHttpException> {
                ProtocolHttpRequestPolicy.normalize(
                    ProtocolHttpRequest(
                        method = "GET",
                        url = url,
                    ),
                )
            }

            assertEquals("Invalid protocol HTTP request: HTTP is not supported", failure.message)
        }
    }

    @Test
    fun `rejects invalid method url and body combinations`() {
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(ProtocolHttpRequest(method = "", url = gridUrl("/login")))
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(ProtocolHttpRequest(method = "GET", url = ""))
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(ProtocolHttpRequest(method = "GET", url = "not a url"))
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(ProtocolHttpRequest(method = "GET", url = "https://::1:443/login"))
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/login"),
                    timeout = GemDelay.ZERO,
                ),
            )
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/login"),
                    timeout = GemDelay.ofMilliseconds(Int.MAX_VALUE.toLong() + 1L),
                ),
            )
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/login"),
                    body = ProtocolHttpBody.TextBody("body"),
                ),
            )
        }
    }

    private companion object {
        fun gridUrl(path: String): String = "https" + "://grid.example$path"

        fun redactedGridUrl(): String = gridUrl("/<redacted>")
    }
}
