package cloud.cyberverse.asterion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(val success: Boolean, val data: T)

@Serializable
data class FootballTeam(
    val name: String,
    @SerialName("badgeURL") val badgeUrl: String? = null,
)

@Serializable
data class FootballTeams(
    val home: FootballTeam? = null,
    val away: FootballTeam? = null,
)

@Serializable
data class FootballStreamSource(
    val source: String,
    val id: String,
)

@Serializable
data class FootballMatch(
    val id: String,
    val title: String,
    @SerialName("date") val kickoffMillis: Long,
    @SerialName("posterURL") val posterUrl: String? = null,
    val popular: Boolean = false,
    val isLive: Boolean = false,
    val teams: FootballTeams? = null,
    val sources: List<FootballStreamSource> = emptyList(),
) {
    val displayTitle: String
        get() {
            val home = teams?.home?.name?.trim()
            val away = teams?.away?.name?.trim()
            return if (!home.isNullOrEmpty() && !away.isNullOrEmpty()) "$home vs $away" else title
        }
}

@Serializable
data class FootballStream(
    val id: String,
    @SerialName("streamNo") val streamNumber: Int,
    val language: String,
    val hd: Boolean,
    @SerialName("embedUrl") val embedUrl: String,
    val source: String,
) {
    val displayName: String
        get() = listOfNotNull(source.replaceFirstChar { it.uppercase() }, language.takeIf { it.isNotEmpty() }, "HD".takeIf { hd })
            .joinToString(" · ")
}

@Serializable
data class FootballStreamCollection(
    val streams: List<FootballStream> = emptyList(),
)

@Serializable
data class FootballStreamRequest(
    val matchId: String,
    val sources: List<FootballStreamSource>,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
)
