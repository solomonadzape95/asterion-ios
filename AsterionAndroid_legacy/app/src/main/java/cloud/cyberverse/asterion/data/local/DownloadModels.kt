package cloud.cyberverse.asterion.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadContentType { MOVIE, ANIME, NOVEL }

@Serializable
enum class DownloadPhase { QUEUED, DOWNLOADING, COMPLETED, FAILED }

@Serializable
data class DownloadSubtitleFile(val label: String, val localUri: String, val language: String?)

/**
 * One row in the unified download index - the single source of truth the Downloads screen reads,
 * whether the underlying bytes are being moved by Media3's DownloadManager (movie/anime video) or
 * a WorkManager job (novel chapter text). Mirrors AsterionMac's MediaDownloadRecord/OfflineDownload
 * split, but flattened into one shape since Android only needs it for display + lookup, not replay.
 */
@Serializable
data class DownloadEntry(
    val id: String,
    val contentType: DownloadContentType,
    val contentId: String,
    val contentTitle: String,
    val unitId: String? = null,
    val unitTitle: String? = null,
    val imageUrl: String? = null,
    val quality: String? = null,
    val phase: DownloadPhase = DownloadPhase.QUEUED,
    val progress: Float = 0f,
    val localUri: String? = null,
    val subtitles: List<DownloadSubtitleFile> = emptyList(),
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isActive: Boolean get() = phase == DownloadPhase.QUEUED || phase == DownloadPhase.DOWNLOADING
    val isAvailableOffline: Boolean get() = phase == DownloadPhase.COMPLETED && localUri != null

    companion object {
        fun movieId(slug: String) = "movie:$slug"
        fun animeId(animeId: String, episodeNumber: Int) = "anime:$animeId:$episodeNumber"
        fun novelId(novelId: String, chapterNumber: Int) = "novel:$novelId:$chapterNumber"
    }
}
