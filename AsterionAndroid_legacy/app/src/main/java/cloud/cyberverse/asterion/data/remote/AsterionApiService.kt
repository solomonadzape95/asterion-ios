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
    suspend fun chapters(@Path("id") novelId: String): ListEnvelope<Chapter>

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
