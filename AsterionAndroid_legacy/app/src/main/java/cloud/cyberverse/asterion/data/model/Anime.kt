package cloud.cyberverse.asterion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeTitle(
    val slug: String,
    val title: String,
    @SerialName("japanese_title") val japaneseTitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val type: String? = null,
    @SerialName("episode_label") val episodeLabel: String? = null,
)

@Serializable
data class AnimeShow(
    val id: String,
    val slug: String,
    val title: String,
    @SerialName("japanese_title") val japaneseTitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val description: String? = null,
    val type: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("sub_episodes") val subEpisodes: Int? = null,
    @SerialName("dub_episodes") val dubEpisodes: Int? = null,
    val studio: String? = null,
    @SerialName("mal_score") val malScore: String? = null,
)

@Serializable
data class AnimeRelatedSeason(
    val id: String,
    val title: String,
    val slug: String,
    val type: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("episodes_count") val episodesCount: Int = 0,
)

@Serializable
data class AnimeEpisode(
    val id: String,
    @SerialName("anime_id") val animeId: String,
    val number: Int,
)

@Serializable
data class AnimeSubtitleTrack(
    @SerialName("file") val fileUrl: String,
    val label: String? = null,
    val kind: String? = null,
    @SerialName("srclang") val languageCode: String? = null,
    @SerialName("default") val isDefault: Boolean = false,
)

@Serializable
data class AnimeStreamSource(
    val server: String,
    @SerialName("url") val embedUrl: String,
    val quality: String? = null,
    @SerialName("source") val directUrl: String? = null,
    val tracks: List<AnimeSubtitleTrack> = emptyList(),
)
