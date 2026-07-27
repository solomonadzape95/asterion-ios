package cloud.cyberverse.asterion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MediaAccountType {
    @SerialName("anime") ANIME,
    @SerialName("movie") MOVIE,
    @SerialName("football") FOOTBALL,
}

@Serializable
data class LibraryRecord(
    val id: String,
    val userId: String,
    val novelId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class AddToLibraryRequest(val novelId: String)

@Serializable
data class DeletedResult(val deleted: Boolean)

@Serializable
data class ReadingProgressRecord(
    val id: String,
    val userId: String,
    val novelId: String,
    val chapterId: String,
    val currentLine: Int,
    val totalLines: Int,
    val percentage: Double,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SaveProgressRequest(
    val novelId: String,
    val chapterId: String,
    val currentLine: Int,
    val totalLines: Int,
    val percentage: Double,
)

@Serializable
data class MediaBookmarkRecord(
    val id: String,
    val userId: String,
    val mediaType: MediaAccountType,
    val contentId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val isSaved: Boolean = true,
    val clientUpdatedAt: String? = null,
)

@Serializable
data class MediaBookmarkRequest(
    val mediaType: MediaAccountType,
    val contentId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val clientEventAt: String,
)

@Serializable
data class MediaBookmarkMutationResult(
    val bookmark: MediaBookmarkRecord? = null,
    val isSaved: Boolean,
    val clientUpdatedAt: String? = null,
)

@Serializable
data class MediaPlaybackProgressRecord(
    val id: String,
    val userId: String,
    val mediaType: MediaAccountType,
    val contentId: String,
    val title: String,
    val imageUrl: String? = null,
    val unitId: String,
    val unitTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionSeconds: Double,
    val durationSeconds: Double,
    val percentage: Double,
    val completed: Boolean,
)

@Serializable
data class MediaProgressRequest(
    val mediaType: MediaAccountType,
    val contentId: String,
    val title: String,
    val imageUrl: String? = null,
    val unitId: String,
    val unitTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionSeconds: Double,
    val durationSeconds: Double,
    val completed: Boolean? = null,
    val started: Boolean = false,
    val sessionId: String,
    val clientEventAt: String,
)

@Serializable
data class MediaHistoryRecord(
    val id: String,
    val userId: String,
    val mediaType: MediaAccountType,
    val contentId: String,
    val title: String,
    val imageUrl: String? = null,
    val unitId: String,
    val unitTitle: String? = null,
    val percentage: Double,
    val completed: Boolean,
    val lastViewedAt: String? = null,
)

@Serializable
data class MediaAccountStats(
    val savedAnime: Int = 0,
    val savedMovies: Int = 0,
    val savedMatches: Int = 0,
    val animeEpisodesCompleted: Int = 0,
    val movieUnitsCompleted: Int = 0,
    val titlesInProgress: Int = 0,
    val historyEntries: Int = 0,
    val activityLast30Days: Int = 0,
)

@Serializable
data class MediaAccountSnapshot(
    val bookmarks: List<MediaBookmarkRecord> = emptyList(),
    val progress: List<MediaPlaybackProgressRecord> = emptyList(),
    val history: List<MediaHistoryRecord> = emptyList(),
    val stats: MediaAccountStats = MediaAccountStats(),
)

@Serializable
data class MediaProgressSaveResult(
    val progress: MediaPlaybackProgressRecord? = null,
    val history: MediaHistoryRecord? = null,
)
