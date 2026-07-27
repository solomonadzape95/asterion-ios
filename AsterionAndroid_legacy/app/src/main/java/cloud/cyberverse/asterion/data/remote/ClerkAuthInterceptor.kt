package cloud.cyberverse.asterion.data.remote

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches a fresh Clerk session JWT to requests against the main Asterion API - the anime/movie/
 * football scraper services are unauthenticated and share the same OkHttpClient (one client keeps
 * connection pooling/timeouts consistent across all four APIs), so this checks the request host
 * rather than needing a second client just for auth.
 *
 * `runBlocking` around the suspend token fetch is safe here: OkHttp interceptors always run on a
 * background dispatcher thread already, never the caller's original thread (Retrofit's suspend
 * call machinery hands off to OkHttp's own executor), so this can't block the main thread.
 */
class ClerkAuthInterceptor(private val apiHost: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.host != apiHost) return chain.proceed(original)

        val session = Clerk.session ?: return chain.proceed(original)
        val jwt = runBlocking { session.fetchToken() }
            .let { it as? ClerkResult.Success }
            ?.value
            ?.jwt
            ?: return chain.proceed(original)

        return chain.proceed(original.newBuilder().header("Authorization", "Bearer $jwt").build())
    }
}
