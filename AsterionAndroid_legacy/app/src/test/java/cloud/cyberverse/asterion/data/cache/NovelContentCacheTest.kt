package cloud.cyberverse.asterion.data.cache

import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NovelContentCacheTest {

    @Test
    fun `returns what was stored`() = runBlocking {
        val cache = NovelContentCache()

        cache.putNovel("1", novel("Sample"))
        cache.putChapters("1", listOf(chapter(1), chapter(2)))

        assertEquals("Sample", cache.novel("1")?.title)
        assertEquals(2, cache.chapters("1")?.size)
    }

    @Test
    fun `misses for an unknown novel`() = runBlocking {
        val cache = NovelContentCache()

        assertNull(cache.novel("nope"))
        assertNull(cache.chapters("nope"))
    }

    @Test
    fun `does not cache an empty chapter list`() = runBlocking {
        // A truncated or failed crawl returns empty; caching it would make the novel look
        // chapterless for the whole TTL.
        val cache = NovelContentCache()

        cache.putChapters("1", emptyList())

        assertNull(cache.chapters("1"))
    }

    @Test
    fun `expires entries once the ttl passes`() = runBlocking {
        val cache = NovelContentCache(ttl = 30.milliseconds)

        cache.putNovel("1", novel("Sample"))
        cache.putChapters("1", listOf(chapter(1)))
        Thread.sleep(60)

        assertNull(cache.novel("1"))
        assertNull(cache.chapters("1"))
    }

    @Test
    fun `invalidate drops both metadata and chapters`() = runBlocking {
        val cache = NovelContentCache(ttl = 5.minutes)

        cache.putNovel("1", novel("Sample"))
        cache.putChapters("1", listOf(chapter(1)))
        cache.invalidate("1")

        assertNull(cache.novel("1"))
        assertNull(cache.chapters("1"))
    }

    @Test
    fun `keeps novels separate`() = runBlocking {
        val cache = NovelContentCache()

        cache.putChapters("1", listOf(chapter(1)))
        cache.putChapters("2", listOf(chapter(1), chapter(2)))

        assertEquals(1, cache.chapters("1")?.size)
        assertEquals(2, cache.chapters("2")?.size)
    }

    private fun novel(title: String) = Novel(id = "1", title = title)

    private fun chapter(number: Int) = Chapter(
        id = "chapter-$number",
        chapterNumber = number,
        title = "Chapter $number",
    )
}
