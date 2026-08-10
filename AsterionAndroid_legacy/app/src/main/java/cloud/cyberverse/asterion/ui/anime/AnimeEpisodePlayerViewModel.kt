package cloud.cyberverse.asterion.ui.anime

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.ui.common.userMessage
import cloud.cyberverse.asterion.data.model.MediaAccountType
import cloud.cyberverse.asterion.data.model.MediaProgressRequest
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import cloud.cyberverse.asterion.data.sync.nowIso
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import cloud.cyberverse.asterion.ui.components.toMediaItem
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnimeEpisodePlayerState {
    data object Loading : AnimeEpisodePlayerState
    data class Ready(
        val player: ExoPlayer,
        val sources: List<PlaybackSource>,
        val selectedIndex: Int,
        val playbackError: String? = null,
    ) : AnimeEpisodePlayerState
    data class Error(val message: String) : AnimeEpisodePlayerState
}

private const val PROGRESS_REPORT_INTERVAL_MS = 15_000L

class AnimeEpisodePlayerViewModel(
    private val api: AnimeApiService,
    private val mediaAccountRepository: MediaAccountRepository,
    private val animeId: String,
    private val episodeNumber: Int,
    private val showTitle: String,
    private val showImageUrl: String?,
    context: Context,
    videoDownloadManager: VideoDownloadManager,
) : ViewModel() {
    // Routing through the download cache means a downloaded episode plays back from disk
    // automatically (same URI = same cache key) with no separate offline/online branch needed;
    // anything not downloaded just streams straight through to the network as before.
    // DefaultMediaSourceFactory (not a bare HlsMediaSource.Factory) is required here: it's the
    // only factory that merges MediaItem.subtitleConfigurations into the playable source via
    // MergingMediaSource - a raw HlsMediaSource.Factory silently ignores sideloaded subtitles.
    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(videoDownloadManager.cacheDataSourceFactory))
        .build()
    private val _state = MutableStateFlow<AnimeEpisodePlayerState>(AnimeEpisodePlayerState.Loading)
    val state: StateFlow<AnimeEpisodePlayerState> = _state.asStateFlow()

    private var sources: List<PlaybackSource> = emptyList()
    private var selectedIndex = 0

    // See MoviePlayerViewModel's identical failedIndices/handlePlaybackError for why: on a
    // playback error we silently fall through to the next untried source rather than dead-ending
    // the player, only surfacing an error (while keeping the source picker usable) once every
    // source has failed.
    private val failedIndices = mutableSetOf<Int>()

    // Account sync (best-effort - see MediaAccountRepository): one id per open of this player,
    // matching AsterionMac's per-watch-session id; `started` flags only the very first report.
    private val sessionId = UUID.randomUUID().toString()
    private var hasReportedStart = false

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })

        viewModelScope.launch {
            try {
                sources = fetchStreamWithRetry().playbackSources()
                if (sources.isEmpty()) {
                    _state.value = AnimeEpisodePlayerState.Error("No direct playback source for this episode.")
                    return@launch
                }
                selectedIndex = 0
                playSource(selectedIndex)
            } catch (error: Exception) {
                _state.value = AnimeEpisodePlayerState.Error(error.userMessage())
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(PROGRESS_REPORT_INTERVAL_MS)
                reportProgress()
            }
        }
    }

    private fun reportProgress() {
        val durationMs = player.duration
        if (durationMs <= 0) return
        val positionSeconds = player.currentPosition / 1000.0
        val durationSeconds = durationMs / 1000.0
        viewModelScope.launch {
            mediaAccountRepository.reportProgress(
                MediaProgressRequest(
                    mediaType = MediaAccountType.ANIME,
                    contentId = animeId,
                    title = showTitle,
                    imageUrl = showImageUrl,
                    unitId = episodeNumber.toString(),
                    unitTitle = "Episode $episodeNumber",
                    episodeNumber = episodeNumber,
                    positionSeconds = positionSeconds,
                    durationSeconds = durationSeconds,
                    completed = positionSeconds / durationSeconds >= 0.9,
                    started = !hasReportedStart,
                    sessionId = sessionId,
                    clientEventAt = nowIso(),
                ),
            )
            hasReportedStart = true
        }
    }

    fun selectSource(index: Int) {
        if (index !in sources.indices || index == selectedIndex) return
        failedIndices.clear()
        selectedIndex = index
        playSource(index, resume = true)
    }

    fun retryCurrentSource() {
        failedIndices.remove(selectedIndex)
        playSource(selectedIndex)
    }

    private fun handlePlaybackError(error: PlaybackException) {
        failedIndices.add(selectedIndex)
        val nextIndex = sources.indices.firstOrNull { it !in failedIndices }
        if (nextIndex != null) {
            selectedIndex = nextIndex
            playSource(nextIndex)
        } else {
            _state.value = AnimeEpisodePlayerState.Ready(
                player = player,
                sources = sources,
                selectedIndex = selectedIndex,
                playbackError = error.message ?: "Every available source failed to play.",
            )
        }
    }

    private fun playSource(index: Int, resume: Boolean = false) {
        val position = if (resume) player.currentPosition else 0L
        val wasPlaying = player.playWhenReady
        player.setMediaItem(sources[index].toMediaItem(), position)
        player.prepare()
        player.playWhenReady = if (resume) wasPlaying else true
        _state.value = AnimeEpisodePlayerState.Ready(player, sources, selectedIndex)
    }

    // The scraper occasionally stalls on a single attempt; one retry clears most of those.
    private suspend fun fetchStreamWithRetry() = try {
        api.stream(animeId, episodeNumber)
    } catch (error: Exception) {
        api.stream(animeId, episodeNumber)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
