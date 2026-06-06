package org.hostess.protocol.libomv.transport

import org.hostess.core.domain.HostessDelay
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
                timeout = HostessDelay.ofSeconds(5),
                redactionKeys = setOf("Authorization"),
            ),
        )

        assertEquals("POST", result.method)
        assertEquals(HostessDelay.ofSeconds(5), result.timeout)
        assertEquals(redactedGridUrl(), result.redactedTarget)
        val summary = result.redactedSummary(202, setOf("Set-Cookie", "X-Reply"))
        assertTrue(summary.contains("Authorization=<redacted>"))
        assertTrue(summary.contains("X-Proof=<present:3>"))
        assertFalse(summary.contains("secret-token"))
        assertFalse(summary.contains("secret-body"))
        assertFalse(summary.contains("/login"))
    }

    @Test
    fun `allows plain HTTP only for local test servers`() {
        val result = ProtocolHttpRequestPolicy.normalize(
            ProtocolHttpRequest(
                method = "GET",
                url = "http://127.0.0.1:8123/redirect",
            ),
        )

        assertEquals("http://127.0.0.1:8123/<redacted>", result.redactedTarget)
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(
                ProtocolHttpRequest(
                    method = "GET",
                    url = "http://grid.example/login",
                ),
            )
        }

        val ipv6LocalResult = ProtocolHttpRequestPolicy.normalize(
            ProtocolHttpRequest(
                method = "GET",
                url = "http://[::1]:8123/redirect",
            ),
        )
        assertEquals("http://[::1]:8123/<redacted>", ipv6LocalResult.redactedTarget)
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
                    timeout = HostessDelay.ZERO,
                ),
            )
        }
        assertFailsWith<ProtocolHttpException> {
            ProtocolHttpRequestPolicy.normalize(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/login"),
                    timeout = HostessDelay.ofMilliseconds(Int.MAX_VALUE.toLong() + 1L),
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
