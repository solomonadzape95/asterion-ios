package cloud.cyberverse.asterion.data.remote

import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.ListEnvelope
import cloud.cyberverse.asterion.data.model.ListMeta
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The old crawl treated any short page as end-of-list, so a hiccup mid-catalog silently dropped
 * every chapter after it and the screen presented the remainder as the whole novel. These pin the
 * distinction between "finished" and "stopped early".
 */
class FetchAllChaptersTest {

    @Test
    fun `walks every page and reports completion`() = runBlocking {
        val api = FakeApi(totalChapters = 250)

        val crawl = api.fetchAllChapters("1")

        assertEquals(250, crawl.chapters.size)
        assertEquals(250, crawl.total)
        assertTrue(crawl.isComplete)
        assertEquals(3, api.requests)
    }

    @Test
    fun `a short page before the total is reported as incomplete, not silently truncated`() = runBlocking {
        // The server says 250 exist but hands back a partial page at offset 100.
        val api = FakeApi(totalChapters = 250, shortPageAtOffset = 100)

        val crawl = api.fetchAllChapters("1")

        assertFalse("must not claim a truncated list is complete", crawl.isComplete)
        assertTrue(crawl.chapters.size < 250)
        // The count still reflects what the server says exists, so the UI can say "x of 250".
        assertEquals(250, crawl.total)
    }

    @Test
    fun `stops when a page repeats rows instead of advancing`() = runBlocking {
        val api = FakeApi(totalChapters = 250, repeatPageAtOffset = 100)

        val crawl = api.fetchAllChapters("1")

        assertFalse(crawl.isComplete)
        // Would otherwise loop forever re-reading the same offset.
        assertTrue("made ${api.requests} requests", api.requests < 10)
    }

    @Test
    fun `an exact multiple of the page size does not fetch forever`() = runBlocking {
        val api = FakeApi(totalChapters = 200)

        val crawl = api.fetchAllChapters("1")

        assertEquals(200, crawl.chapters.size)
        assertTrue(crawl.isComplete)
        assertEquals(2, api.requests)
    }

    @Test
    fun `continues from chapters already rendered instead of refetching them`() = runBlocking {
        val api = FakeApi(totalChapters = 250)
        val firstPage = api.fetchChapterPage("1")
        api.requests = 0

        val crawl = api.fetchAllChapters("1", startingFrom = firstPage.chapters)

        assertEquals(250, crawl.chapters.size)
        assertTrue(crawl.isComplete)
        // Only the two remaining pages, not all three again.
        assertEquals(2, api.requests)
    }

    @Test
    fun `single page novel completes in one request`() = runBlocking {
        val api = FakeApi(totalChapters = 12)

        val crawl = api.fetchAllChapters("1")

        assertEquals(12, crawl.chapters.size)
        assertTrue(crawl.isComplete)
        assertEquals(1, api.requests)
    }

    @Test
    fun `first page returns the server total, not the page size`() = runBlocking {
        val api = FakeApi(totalChapters = 4000)

        val page = api.fetchChapterPage("1")

        assertEquals(100, page.chapters.size)
        // This is the number a reader sees immediately, before the rest arrives.
        assertEquals(4000, page.total)
    }

    /**
     * Only `chapters` is exercised here; everything else on the interface throws if touched, which
     * keeps the fake from silently growing a second implementation of the API.
     */
    private val unusedApi: AsterionApiService = java.lang.reflect.Proxy.newProxyInstance(
        AsterionApiService::class.java.classLoader,
        arrayOf(AsterionApiService::class.java),
    ) { _, method, _ -> throw UnsupportedOperationException(method.name) } as AsterionApiService

    private inner class FakeApi(
        private val totalChapters: Int,
        private val shortPageAtOffset: Int? = null,
        private val repeatPageAtOffset: Int? = null,
    ) : AsterionApiService by unusedApi {
        var requests = 0

        override suspend fun chapters(
            novelId: String,
            limit: Int?,
            offset: Int?,
        ): ListEnvelope<Chapter> {
            requests++
            val pageLimit = limit ?: CHAPTER_PAGE_LIMIT
            val requestedOffset = offset ?: 0
            val start = if (requestedOffset == repeatPageAtOffset) requestedOffset - pageLimit else requestedOffset
            val size = when (requestedOffset) {
                shortPageAtOffset -> pageLimit / 2
                else -> minOf(pageLimit, (totalChapters - start).coerceAtLeast(0))
            }
            return ListEnvelope(
                data = (0 until size).map { index -> chapter(start + index + 1) },
                meta = ListMeta(count = size, total = totalChapters, offset = requestedOffset, limit = pageLimit),
            )
        }

        private fun chapter(number: Int) =
            Chapter(id = "chapter-$number", chapterNumber = number, title = "Chapter $number")
    }
}
