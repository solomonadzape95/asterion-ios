package cloud.cyberverse.asterion.data.remote

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries transient upstream failures before the app ever hears about them.
 *
 * The scrapers map *every* internal exception to a 502 (see services/anime/app.py and
 * services/movies/app.py), so a Cloudflare challenge, a rotated source domain, or a cold cache all
 * surface as 502 even though the very next request usually succeeds. OkHttp does not retry these:
 * `retryOnConnectionFailure` only covers route-level reconnects, never 5xx or a read timeout.
 *
 * Only idempotent methods are retried, so a retry can never double-submit a write.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val initialBackoffMillis: Long = 500L,
    private val sleep: (Long) -> Unit = Thread::sleep,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.method.isIdempotent()) return chain.proceed(request)

        var lastFailure: IOException? = null

        repeat(maxRetries + 1) { attempt ->
            if (attempt > 0) {
                // A cancelled call must not keep sleeping and retrying in the background.
                if (chain.call().isCanceled()) throw IOException("Canceled")
                sleep(backoffMillis(attempt))
            }

            try {
                val response = chain.proceed(request)
                if (!response.isRetryable() || attempt == maxRetries) return response
                // The body must be closed before the connection can be reused for the retry.
                response.close()
            } catch (timeout: SocketTimeoutException) {
                lastFailure = timeout
            } catch (interrupted: InterruptedIOException) {
                // "Canceled" and callTimeout exhaustion both land here - neither should be retried.
                throw interrupted
            } catch (error: IOException) {
                lastFailure = error
            }
        }

        throw lastFailure ?: IOException("Request failed after ${maxRetries + 1} attempts")
    }

    /** Exponential, with jitter so a burst of failed calls doesn't retry in lockstep. */
    private fun backoffMillis(attempt: Int): Long {
        val exponential = initialBackoffMillis shl (attempt - 1)
        return exponential + Random.nextLong(0, initialBackoffMillis)
    }

    private fun String.isIdempotent(): Boolean = this == "GET" || this == "HEAD"

    private fun Response.isRetryable(): Boolean = code == 502 || code == 503 || code == 504
}
