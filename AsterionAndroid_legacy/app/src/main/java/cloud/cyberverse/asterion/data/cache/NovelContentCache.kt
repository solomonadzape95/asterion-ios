package cloud.cyberverse.asterion.data.cache

import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-lifetime cache for novel metadata and chapter lists.
 *
 * Two things made reopening a novel feel broken:
 *
 * 1. Popping the back stack destroys the ViewModel, so returning to a novel re-ran the whole load -
 *    including `fetchAllChapters`, a *sequential* page crawl. A 4,000-chapter novel is 40 blocking
 *    round trips before anything renders.
 * 2. The reader re-ran that same crawl on every chapter open, just to populate prev/next.
 *
 * Holding the result here means the second visit is instant and the reader shares the detail
 * screen's work instead of repeating it. This is deliberately in-memory: it survives navigation,
 * which is the reported problem, and dies with the process, so there is no staleness to manage
 * across launches. The HTTP disk cache covers cold starts.
 */
class NovelContentCache(private val ttl: Duration = 15.minutes) {

    private data class Entry<T>(val value: T, val storedAt: Long) {
        fun isFresh(ttl: Duration, now: Long) = now - storedAt < ttl.inWholeMilliseconds
    }

    private val mutex = Mutex()
    private val novels = mutableMapOf<String, Entry<Novel>>()
    private val chapters = mutableMapOf<String, Entry<List<Chapter>>>()

    suspend fun novel(novelId: String): Novel? = mutex.withLock {
        novels[novelId]?.takeIf { it.isFresh(ttl, now()) }?.value
    }

    suspend fun chapters(novelId: String): List<Chapter>? = mutex.withLock {
        chapters[novelId]?.takeIf { it.isFresh(ttl, now()) }?.value
    }

    suspend fun putNovel(novelId: String, novel: Novel): Unit = mutex.withLock {
        novels[novelId] = Entry(novel, now())
    }

    suspend fun putChapters(novelId: String, value: List<Chapter>): Unit = mutex.withLock {
        // An empty list means the crawl failed or was truncated; caching it would pin the failure
        // for the whole TTL and make the novel look chapterless until the app restarts.
        if (value.isNotEmpty()) chapters[novelId] = Entry(value, now())
    }

    /** Called after a refresh so the next open re-reads rather than serving what we just replaced. */
    suspend fun invalidate(novelId: String): Unit = mutex.withLock {
        novels.remove(novelId)
        chapters.remove(novelId)
    }

    private fun now() = System.currentTimeMillis()
}
