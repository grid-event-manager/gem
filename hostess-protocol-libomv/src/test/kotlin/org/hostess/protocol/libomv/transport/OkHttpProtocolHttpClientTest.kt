package org.hostess.protocol.libomv.transport

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import org.hostess.core.domain.HostessDelay
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
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
            HostessDelay.ofMilliseconds(-1)
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
                timeout = HostessDelay.ofMilliseconds(750),
            ),
        )

        assertEquals(TimeUnit.MILLISECONDS.toNanos(750), capturedTimeoutNanos.get())
    }

    @Test
    fun `maps binary upload body`() {
        val uploaded = byteArrayOf(9, 8, 7, 6)
        val client = OkHttpProtocolHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val body = Buffer().also { request.body?.writeTo(it) }.readByteArray()

                    assertEquals("PUT", request.method)
                    assertEquals("application/octet-stream", request.body?.contentType().toString())
                    assertContentEquals(uploaded, body)

                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("uploaded".toResponseBody())
                        .build()
                }
                .build(),
        )

        val response = client.execute(
            ProtocolHttpRequest(
                method = "PUT",
                url = gridUrl("/upload"),
                body = ProtocolHttpBody.BinaryUploadBody(uploaded),
            ),
        )

        assertEquals(200, response.statusCode)
        assertContentEquals("uploaded".encodeToByteArray(), response.body)
    }

    @Test
    fun `follows redirects and uses supplied cookie jar on local test server`() {
        LocalRedirectServer().use { server ->
            val cookieJar = MemoryCookieJar()
            val client = OkHttpProtocolHttpClient(
                OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build(),
            )

            val response = client.execute(
                ProtocolHttpRequest(
                    method = "GET",
                    url = server.url("/redirect"),
                    timeout = HostessDelay.ofSeconds(2),
                ),
            )

            assertEquals(200, response.statusCode)
            assertContentEquals("final".encodeToByteArray(), response.body)
            assertEquals(listOf("proof=ok"), server.seenCookies)
        }
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

    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.removeAll { stored -> cookies.any { it.name == stored.name } }
            this.cookies += cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookies.filter { it.matches(url) }
    }

    private class LocalRedirectServer : AutoCloseable {
        private val executor = Executors.newSingleThreadExecutor()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val seenCookies: MutableList<String> = CopyOnWriteArrayList()

        init {
            server.createContext("/redirect") { exchange ->
                exchange.responseHeaders.add("Location", "/final")
                exchange.responseHeaders.add("Set-Cookie", "proof=ok; Path=/")
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            server.createContext("/final") { exchange ->
                exchange.requestHeaders.getFirst("Cookie")?.let(seenCookies::add)
                val body = "final".encodeToByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.executor = executor
            server.start()
        }

        fun url(path: String): String = "http" + "://127.0.0.1:${server.address.port}$path"

        override fun close() {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    private companion object {
        fun gridUrl(path: String): String = "https" + "://grid.example$path"

        fun plainGridUrl(path: String): String = "http" + "://grid.example$path"

        fun redactedGridUrl(): String = gridUrl("/<redacted>")
    }
}
