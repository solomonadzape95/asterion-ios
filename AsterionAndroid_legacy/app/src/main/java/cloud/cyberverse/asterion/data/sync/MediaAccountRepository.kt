package cloud.cyberverse.asterion.data.sync

import cloud.cyberverse.asterion.data.model.MediaAccountSnapshot
import cloud.cyberverse.asterion.data.model.MediaAccountType
import cloud.cyberverse.asterion.data.model.MediaBookmarkRequest
import cloud.cyberverse.asterion.data.model.MediaProgressRequest
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

fun nowIso(): String = Instant.now().toString()

/**
 * A single in-memory cache of the signed-in user's media account snapshot (bookmarks/progress/
 * history/stats from /me/media), shared across every screen that needs it - mirrors AsterionMac's
 * MediaActivityStore. Without this, each detail screen would need its own full-snapshot GET just
 * to answer "is this bookmarked?", and Profile would have no way to show real numbers at all.
 *
 * All network calls here are best-effort (wrapped in runCatching): sync is a nice-to-have layered
 * on top of a working offline-first app, not something that should ever block or crash a screen
 * when the network hiccups or the account has no data yet.
 */
class MediaAccountRepository(private val api: AsterionApiService) {
    private val _snapshot = MutableStateFlow<MediaAccountSnapshot?>(null)
    val snapshot: StateFlow<MediaAccountSnapshot?> = _snapshot.asStateFlow()

    suspend fun refresh(): MediaAccountSnapshot? {
        val result = runCatching { api.mediaAccount().data }.getOrNull()
        if (result != null) _snapshot.value = result
        return result
    }

    fun isBookmarked(mediaType: MediaAccountType, contentId: String): Boolean =
        _snapshot.value?.bookmarks.orEmpty().any { it.mediaType == mediaType && it.contentId == contentId && it.isSaved }

    suspend fun toggleBookmark(
        mediaType: MediaAccountType,
        contentId: String,
        title: String,
        subtitle: String?,
        imageUrl: String?,
    ): Boolean {
        val wasBookmarked = isBookmarked(mediaType, contentId)
        val request = MediaBookmarkRequest(
            mediaType = mediaType,
            contentId = contentId,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            clientEventAt = nowIso(),
        )
        val result = runCatching {
            if (wasBookmarked) api.deleteMediaBookmark(request).data else api.saveMediaBookmark(request).data
        }.getOrNull()
        val nowSaved = result?.isSaved ?: wasBookmarked

        // Patch the cached snapshot in place so every other screen sharing this repository (e.g.
        // Profile's stats) reflects the change immediately, without a full re-fetch.
        _snapshot.update { snapshot ->
            snapshot ?: return@update snapshot
            val withoutThisItem = snapshot.bookmarks.filterNot { it.mediaType == mediaType && it.contentId == contentId }
            val updatedBookmark = result?.bookmark
            snapshot.copy(bookmarks = if (nowSaved && updatedBookmark != null) withoutThisItem + updatedBookmark else withoutThisItem)
        }
        return nowSaved
    }

    /** Fire-and-forget playback progress report - failures are swallowed since this runs
     * periodically during playback and a dropped update just means the next one catches up. */
    suspend fun reportProgress(request: MediaProgressRequest) {
        runCatching { api.saveMediaProgress(request) }
    }
}
