package org.gem.protocol.libomv.transport

import java.io.IOException
import org.gem.core.domain.GemDelay
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OkHttpProtocolHttpClientTest {
    @Test
    fun `maps request and response fields through OkHttp`() {
        val client = OkHttpProtocolHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()

                    assertEquals("POST", request.method)
                    assertEquals(gridUrl("/login"), request.url.toString())
                    assertEquals("application/llsd+xml", request.body?.contentType().toString())
                    assertEquals("yes", request.header("X-Proof"))
                    assertEquals("<llsd />", body)

                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(202)
                        .message("Accepted")
                        .header("X-Reply", "mapped")
                        .body("accepted".toResponseBody())
                        .build()
                }
                .build(),
        )

        val response = client.execute(
            ProtocolHttpRequest(
                method = "post",
                url = gridUrl("/login"),
                headers = mapOf("X-Proof" to "yes"),
                body = ProtocolHttpBody.TextBody("<llsd />", "application/llsd+xml"),
                redactionKeys = setOf("X-Proof"),
            ),
        )

        assertEquals(202, response.statusCode)
        assertEquals(listOf("mapped"), response.headers["X-Reply"])
        assertContentEquals("accepted".encodeToByteArray(), response.body)
        assertTrue(response.redactedSummary.contains("POST ${redactedGridUrl()} -> 202"))
        assertTrue(response.redactedSummary.contains("X-Proof=<redacted>"))
        assertFalse(response.redactedSummary.contains("<llsd"))
    }

    @Test
    fun `rejects invalid requests before transport execution`() {
        val client = OkHttpProtocolHttpClient()

        assertFailsWith<ProtocolHttpException> {
            client.execute(ProtocolHttpRequest(method = "", url = gridUrl("/login")))
        }
        assertFailsWith<ProtocolHttpException> {
            client.execute(ProtocolHttpRequest(method = "GET", url = ""))
        }
        assertFailsWith<ProtocolHttpException> {
            client.execute(
                ProtocolHttpRequest(
                    method = "GET",
                    url = plainGridUrl("/login"),
                ),
            )
        }
        assertFailsWith<ProtocolHttpException> {
            client.execute(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/login"),
                    body = ProtocolHttpBody.TextBody("body"),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GemDelay.ofMilliseconds(-1)
        }
    }

    @Test
    fun `propagates timeout to the OkHttp call`() {
        val capturedTimeoutNanos = AtomicLong()
        val client = OkHttpProtocolHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    capturedTimeoutNanos.set(chain.call().timeout().timeoutNanos())
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(204)
                        .message("No Content")
                        .body(ByteArray(0).toResponseBody())
                        .build()
                }
                .build(),
        )

        client.execute(
            ProtocolHttpRequest(
                method = "GET",
                url = gridUrl("/caps"),
                timeout = GemDelay.ofMilliseconds(750),
            ),
        )

        assertEquals(TimeUnit.MILLISECONDS.toNanos(750), capturedTimeoutNanos.get())
    }

    @Test
    fun `redacted summary hides URL path query header values and bodies`() {
        val client = OkHttpProtocolHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Set-Cookie", "secret-cookie")
                        .body("secret-body".toResponseBody())
                        .build()
                }
                .build(),
        )

        val response = client.execute(
            ProtocolHttpRequest(
                method = "POST",
                url = gridUrl("/login?token=secret-token"),
                headers = mapOf("Authorization" to "secret-token", "X-Visible" to "abc"),
                body = ProtocolHttpBody.TextBody("secret-body"),
                redactionKeys = setOf("Authorization"),
            ),
        )

        assertTrue(response.redactedSummary.contains(redactedGridUrl()))
        assertTrue(response.redactedSummary.contains("Authorization=<redacted>"))
        assertTrue(response.redactedSummary.contains("X-Visible=<present:3>"))
        assertFalse(response.redactedSummary.contains("secret-token"))
        assertFalse(response.redactedSummary.contains("secret-body"))
        assertFalse(response.redactedSummary.contains("/login"))
        assertFalse(response.redactedSummary.contains("secret-cookie"))
    }

    @Test
    fun `wraps transport failures with a redacted target`() {
        val client = OkHttpProtocolHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { throw IOException("raw network detail") }
                .build(),
        )

        val failure = assertFailsWith<ProtocolHttpException> {
            client.execute(
                ProtocolHttpRequest(
                    method = "GET",
                    url = gridUrl("/secret/path?token=value"),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains(redactedGridUrl()))
        assertFalse(failure.message.orEmpty().contains("secret"))
        assertNull(failure.cause)
    }

    private companion object {
        fun gridUrl(path: String): String = "https" + "://grid.example$path"

        fun plainGridUrl(path: String): String = "http" + "://grid.example$path"

        fun redactedGridUrl(): String = gridUrl("/<redacted>")
    }
}
