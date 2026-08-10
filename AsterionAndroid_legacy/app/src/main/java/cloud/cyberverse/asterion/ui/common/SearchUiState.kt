package cloud.cyberverse.asterion.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

const val SEARCH_DEBOUNCE_MS = 400L

/**
 * Search results, kept deliberately separate from whatever catalog state a screen also holds -
 * a failed search must never blank the browse experience behind it.
 */
sealed interface SearchUiState<out T> {
    data object Idle : SearchUiState<Nothing>
    data object Loading : SearchUiState<Nothing>
    data class Loaded<T>(val results: List<T>) : SearchUiState<T>
    data class Error(val message: String) : SearchUiState<Nothing>
}

/**
 * Debounces, then runs [block], writing Loading -> Loaded/Error into [target].
 *
 * The two rules that keep superseded queries from flashing an error on screen:
 *
 * 1. The network call runs *inside* the returned [Job], so cancelling it actually cancels the
 *    request. Launching into viewModelScope instead only cancels the debounce, leaving every
 *    keystroke's request in flight to land in arbitrary order.
 * 2. A cancelled job never writes. `CancellationException` is rethrown rather than rendered, and
 *    [ensureActive] runs after the call returns so a job cancelled while its response was already
 *    decoding can't write a stale result over a newer one.
 *
 * Callers must `searchJob?.cancel()` before calling this.
 */
fun <T> ViewModel.launchSearch(
    target: MutableStateFlow<SearchUiState<T>>,
    query: String,
    debounceMs: Long = SEARCH_DEBOUNCE_MS,
    block: suspend (String) -> List<T>,
): Job? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        target.value = SearchUiState.Idle
        return null
    }
    return viewModelScope.launch {
        delay(debounceMs)
        target.value = SearchUiState.Loading
        try {
            val results = block(trimmed)
            currentCoroutineContext().ensureActive()
            target.value = SearchUiState.Loaded(results)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            target.value = SearchUiState.Error(error.userMessage())
        }
    }
}

/**
 * Transport failures read as "HTTP 502 Bad Gateway" or bare "timeout" straight off the exception.
 * Neither means anything to a reader, and both are usually transient scraper hiccups.
 */
fun Throwable.userMessage(): String = when {
    this is java.net.UnknownHostException -> "You appear to be offline."
    // SocketTimeoutException extends InterruptedIOException, and OkHttp's callTimeout raises the
    // latter directly - both mean "the server never finished", not "we couldn't reach it".
    this is java.io.InterruptedIOException -> "That took too long to load. Try again."
    this is java.io.IOException -> "Couldn't reach Asterion. Check your connection."
    message?.contains("HTTP 5") == true ->
        "Asterion is having trouble right now. Try again in a moment."
    else -> message ?: "Something went wrong."
}
