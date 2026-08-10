package cloud.cyberverse.asterion.data.remote

import cloud.cyberverse.asterion.data.model.MovieCatalogPage
import cloud.cyberverse.asterion.data.model.MoviePlaybackSources
import cloud.cyberverse.asterion.data.model.MovieEpisode
import cloud.cyberverse.asterion.data.model.MovieShow
import cloud.cyberverse.asterion.data.model.MovieTitle
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {
    @GET("popular/movies")
    suspend fun popular(): List<MovieTitle>

    @GET("movies")
    suspend fun catalog(@Query("page") page: Int = 1): MovieCatalogPage

    @GET("search")
    suspend fun search(@Query("q") query: String): List<MovieTitle>

    @GET("show/{slug}")
    suspend fun show(@Path("slug") slug: String): MovieShow

    /** Freshly resolved + verified sources, unlike [show]'s unverified `streams` field - use this to actually play or download. */
    @GET("show/{slug}/episodes")
    suspend fun showEpisodes(@Path("slug") slug: String): List<MovieEpisode>

    @GET("playback/{slug}")
    suspend fun playback(@Path("slug") slug: String): MoviePlaybackSources
}
