package cloud.cyberverse.asterion.data.remote

import cloud.cyberverse.asterion.data.model.AnimeEpisode
import cloud.cyberverse.asterion.data.model.AnimeRelatedSeason
import cloud.cyberverse.asterion.data.model.AnimeShow
import cloud.cyberverse.asterion.data.model.AnimeStreamSource
import cloud.cyberverse.asterion.data.model.AnimeTitle
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeApiService {
    @GET("popular")
    suspend fun popular(@Query("page") page: Int = 1): List<AnimeTitle>

    @GET("search")
    suspend fun search(@Query("q") query: String, @Query("page") page: Int = 1): List<AnimeTitle>

    @GET("season")
    suspend fun season(@Query("season") season: String, @Query("year") year: Int, @Query("page") page: Int = 1): List<AnimeTitle>

    @GET("show/{slug}")
    suspend fun show(@Path("slug") slug: String): AnimeShow

    @GET("seasons/{animeId}")
    suspend fun relatedSeasons(@Path("animeId") animeId: String): List<AnimeRelatedSeason>

    @GET("episodes/{animeId}")
    suspend fun episodes(@Path("animeId") animeId: String): List<AnimeEpisode>

    @GET("stream/{animeId}/{episodeNumber}")
    suspend fun stream(@Path("animeId") animeId: String, @Path("episodeNumber") episodeNumber: Int): List<AnimeStreamSource>
}
