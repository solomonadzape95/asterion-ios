package cloud.cyberverse.asterion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieTitle(
    val id: String,
    val slug: String,
    val title: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("imdb_rating") val imdbRating: String? = null,
    val runtime: String? = null,
    val year: String? = null,
    val type: String? = null,
)

@Serializable
data class MovieCatalogPage(
    val page: Int,
    @SerialName("total_pages") val totalPages: Int,
    val results: List<MovieTitle>,
)

@Serializable
data class MovieShow(
    val slug: String,
    val title: String,
    val type: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val description: String? = null,
    @SerialName("imdb_rating") val imdbRating: String? = null,
    @SerialName("tmdb_rating") val tmdbRating: String? = null,
    @SerialName("rotten_tomatoes") val rottenTomatoes: String? = null,
    val metacritic: String? = null,
    val genres: List<String> = emptyList(),
    val director: String? = null,
    val actors: List<String> = emptyList(),
    val duration: String? = null,
    @SerialName("release_year") val releaseYear: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val country: String? = null,
    val seasons: List<String> = emptyList(),
    val streams: List<MovieStreamSource> = emptyList(),
) {
    val isSeries: Boolean get() = type.contains("tv", ignoreCase = true)
}

@Serializable
data class MovieStreamSource(
    @SerialName("server_id") val serverId: Int,
    val label: String,
    val quality: String,
    @SerialName("embed_url") val embedUrl: String,
    @SerialName("is_hls") val isHls: Boolean,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val automatic: Boolean = false,
    @SerialName("proxy_url") val proxyUrl: String? = null,
)

/** The verified, freshly-resolved sources from /api/playback/{slug} - use this for actual playback and
 * downloads, not [MovieShow.streams], which is unverified and can include dead links. */
@Serializable
data class MoviePlaybackSources(
    val slug: String,
    val sources: List<MovieStreamSource> = emptyList(),
    @SerialName("verified_direct_count") val verifiedDirectCount: Int = 0,
)
