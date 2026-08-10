package cloud.cyberverse.asterion.data.remote

import cloud.cyberverse.asterion.data.model.AddToLibraryRequest
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.DeletedResult
import cloud.cyberverse.asterion.data.model.ItemEnvelope
import cloud.cyberverse.asterion.data.model.LibraryRecord
import cloud.cyberverse.asterion.data.model.ListEnvelope
import cloud.cyberverse.asterion.data.model.MediaAccountSnapshot
import cloud.cyberverse.asterion.data.model.MediaBookmarkMutationResult
import cloud.cyberverse.asterion.data.model.MediaBookmarkRequest
import cloud.cyberverse.asterion.data.model.MediaProgressRequest
import cloud.cyberverse.asterion.data.model.MediaProgressSaveResult
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.data.model.ReadingProgressRecord
import cloud.cyberverse.asterion.data.model.SaveProgressRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface AsterionApiService {
    @GET("novels")
    suspend fun novels(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): ListEnvelope<Novel>

    @GET("novels/{id}")
    suspend fun novel(@Path("id") id: String): ItemEnvelope<Novel>

    @GET("novels/{id}/chapters")
    suspend fun chapters(
        @Path("id") novelId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): ListEnvelope<Chapter>

    @GET("novels/{id}/chapters/{chapterNumber}")
    suspend fun chapter(@Path("id") novelId: String, @Path("chapterNumber") chapterNumber: Int): ItemEnvelope<Chapter>

    // --- Account sync (requires a signed-in Clerk session - see ClerkAuthInterceptor) ---

    @GET("me/library")
    suspend fun library(): ListEnvelope<LibraryRecord>

    @POST("me/library")
    suspend fun addToLibrary(@Body body: AddToLibraryRequest): ItemEnvelope<LibraryRecord>

    @DELETE("me/library/{novelId}")
    suspend fun removeFromLibrary(@Path("novelId") novelId: String): ItemEnvelope<DeletedResult>

    @GET("me/progress")
    suspend fun allProgress(): ListEnvelope<ReadingProgressRecord>

    @GET("me/progress")
    suspend fun progressForNovel(@Query("novelId") novelId: String): ItemEnvelope<ReadingProgressRecord?>

    @PUT("me/progress")
    suspend fun saveProgress(@Body body: SaveProgressRequest): ItemEnvelope<ReadingProgressRecord?>

    @GET("me/media")
    suspend fun mediaAccount(): ItemEnvelope<MediaAccountSnapshot>

    @PUT("me/media/bookmarks")
    suspend fun saveMediaBookmark(@Body body: MediaBookmarkRequest): ItemEnvelope<MediaBookmarkMutationResult?>

    // Retrofit's plain @DELETE annotation has no body support - @HTTP(hasBody = true) is the
    // documented way to send a DELETE with a JSON body, which this endpoint requires (it needs
    // the full descriptor, not just an id, to resolve which bookmark to unset).
    @HTTP(method = "DELETE", path = "me/media/bookmarks", hasBody = true)
    suspend fun deleteMediaBookmark(@Body body: MediaBookmarkRequest): ItemEnvelope<MediaBookmarkMutationResult?>

    @PUT("me/media/progress")
    suspend fun saveMediaProgress(@Body body: MediaProgressRequest): ItemEnvelope<MediaProgressSaveResult>
}

/** The server caps a chapters page at 100. */
const val CHAPTER_PAGE_LIMIT = 100

/**
 * A slice of a novel's chapters plus the server's count of the whole list.
 *
 * [total] is the number to show a reader. The novel record carries its own `totalChapters`, but
 * that is an unparsed string written by the scraper and is routinely wrong or stale; the chapters
 * endpoint counts the rows that actually exist.
 */
data class ChapterPage(
    val chapters: List<Chapter>,
    val total: Int,
)

/**
 * The outcome of walking every chapter page.
 *
 * [isComplete] is the point of this type. The previous crawl treated a short page as "end of list"
 * and stopped, so any hiccup mid-catalog silently dropped every chapter after it and the screen
 * presented the remainder as the whole novel. Callers can now tell a finished list from a
 * truncated one.
 */
data class ChapterCrawl(
    val chapters: List<Chapter>,
    val total: Int,
    val isComplete: Boolean,
)

/** Fetches a single page, for callers that render the first page before the rest arrives. */
suspend fun AsterionApiService.fetchChapterPage(
    novelId: String,
    offset: Int = 0,
    pageSize: Int = CHAPTER_PAGE_LIMIT,
): ChapterPage {
    val limit = pageSize.coerceIn(1, CHAPTER_PAGE_LIMIT)
    val response = chapters(novelId = novelId, limit = limit, offset = offset)
    val reportedTotal = response.meta?.total?.takeIf { it > 0 }
    return ChapterPage(
        chapters = response.data,
        total = reportedTotal ?: (offset + response.data.size),
    )
}

/**
 * Walks every page, driven by the server's reported total rather than by page shape.
 *
 * [startingFrom] lets a caller that already rendered page one continue from where it left off
 * instead of refetching what is already on screen.
 */
suspend fun AsterionApiService.fetchAllChapters(
    novelId: String,
    pageSize: Int = CHAPTER_PAGE_LIMIT,
    startingFrom: List<Chapter> = emptyList(),
): ChapterCrawl {
    val limit = pageSize.coerceIn(1, CHAPTER_PAGE_LIMIT)
    val collected = startingFrom.toMutableList()
    val seenIds = startingFrom.mapTo(mutableSetOf()) { it.id }
    var offset = startingFrom.size
    var total = 0
    var isComplete = false

    while (true) {
        val response = chapters(novelId = novelId, limit = limit, offset = offset)
        val newChapters = response.data.filter { seenIds.add(it.id) }
        collected += newChapters
        total = response.meta?.total?.takeIf { it > 0 } ?: total

        if (total > 0 && collected.size >= total) {
            isComplete = true
            break
        }

        if (response.data.isEmpty()) {
            // No total to aim at (older responses omit meta), so an empty page is the only
            // honest end-of-list signal available.
            isComplete = total <= 0 || collected.size >= total
            break
        }

        if (newChapters.isEmpty()) {
            // Every row on this page was already seen, which means offsets are repeating rather
            // than advancing. Continuing would loop forever over the same rows.
            break
        }

        if (response.data.size < limit) {
            // A short page before reaching the total means the server gave us less than it says
            // exists. Stopping here is right, but claiming the list is complete is not.
            break
        }

        offset += limit
    }

    return ChapterCrawl(
        chapters = collected.sortedBy { it.chapterNumber },
        total = maxOf(total, collected.size),
        isComplete = isComplete,
    )
}
