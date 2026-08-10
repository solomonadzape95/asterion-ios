package cloud.cyberverse.asterion.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The scrapers return 502 for any internal failure, so these cover the "transient 502 that would
 * have surfaced to the reader" path end to end through a real OkHttp stack.
 */
class RetryInterceptorTest {

    private lateinit var server: MockWebServer
    private val slept = mutableListOf<Long>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `retries a 502 and returns the eventual success`() {
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client().newCall(get()).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("ok", response.body?.string())
        }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `gives up after the retry budget and surfaces the last failure`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(502)) }

        client().newCall(get()).execute().use { response ->
            assertEquals(502, response.code)
        }
        // 1 initial attempt + 2 retries, not an unbounded loop.
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `retries a read timeout`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client(readTimeoutMillis = 300).newCall(get()).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `does not retry a 404`() {
        server.enqueue(MockResponse().setResponseCode(404))

        client().newCall(get()).execute().use { response ->
            assertEquals(404, response.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `does not retry non-idempotent methods`() {
        server.enqueue(MockResponse().setResponseCode(502))

        val post = Request.Builder()
            .url(server.url("/thing"))
            .post("{}".toRequestBody())
            .build()

        client().newCall(post).execute().use { response ->
            // Retrying a POST could double-submit, so the 502 is surfaced instead.
            assertEquals(502, response.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `backs off exponentially with jitter`() {
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(200))

        client(initialBackoffMillis = 100L).newCall(get()).execute().close()

        assertEquals(2, slept.size)
        assertTrue("first backoff was ${slept[0]}", slept[0] in 100L..199L)
        assertTrue("second backoff was ${slept[1]}", slept[1] in 200L..299L)
    }

    private fun get() = Request.Builder().url(server.url("/thing")).build()

    private fun client(readTimeoutMillis: Long = 5_000, initialBackoffMillis: Long = 1L) =
        OkHttpClient.Builder()
            .addInterceptor(
                RetryInterceptor(
                    initialBackoffMillis = initialBackoffMillis,
                    sleep = { slept += it },
                ),
            )
            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()
}
