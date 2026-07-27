package cloud.cyberverse.asterion.ui.movies

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.data.model.MediaAccountType
import cloud.cyberverse.asterion.data.model.MediaProgressRequest
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import cloud.cyberverse.asterion.data.sync.nowIso
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import cloud.cyberverse.asterion.ui.components.PlaybackSourceKind
import cloud.cyberverse.asterion.ui.components.toMediaItem
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MoviePlayerState {
    data object Loading : MoviePlayerState
    data class Ready(
        val player: ExoPlayer,
        val sources: List<PlaybackSource>,
        val selectedIndex: Int,
        val playbackError: String? = null,
    ) : MoviePlayerState
    data class Error(val message: String) : MoviePlayerState
}

private const val PROGRESS_REPORT_INTERVAL_MS = 15_000L

class MoviePlayerViewModel(
    private val api: MovieApiService,
    private val mediaAccountRepository: MediaAccountRepository,
    private val slug: String,
    context: Context,
    videoDownloadManager: VideoDownloadManager,
) : ViewModel() {
    // Routing through the download cache means a downloaded title plays back from disk
    // automatically (same URI = same cache key) with no separate offline/online branch needed;
    // anything not downloaded just streams straight through to the network as before. The
    // upstream side of that cache already allows cross-protocol redirects for the movies proxy's
    // occasional http:// sub-playlist links.
    // DefaultMediaSourceFactory (not a bare HlsMediaSource.Factory) is required here: it's the
    // only factory that merges MediaItem.subtitleConfigurations into the playable source via
    // MergingMediaSource - a raw HlsMediaSource.Factory silently ignores sideloaded subtitles.
    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(videoDownloadManager.cacheDataSourceFactory))
        .build()
    private val _state = MutableStateFlow<MoviePlayerState>(MoviePlayerState.Loading)
    val state: StateFlow<MoviePlayerState> = _state.asStateFlow()

    private var sources: List<PlaybackSource> = emptyList()
    private var selectedIndex = 0

    // Tracks which sources have already failed this session so a playback error can silently
    // fall through to the next untried one instead of dead-ending the whole player - only once
    // every source has failed do we surface an error (still keeping the source picker usable).
    private val failedIndices = mutableSetOf<Int>()

    // Account sync (best-effort - see MediaAccountRepository): one id per open of this player,
    // matching AsterionMac's per-watch-session id; `started` flags only the very first report.
    private val sessionId = UUID.randomUUID().toString()
    private var hasReportedStart = false
    private var showTitle: String? = null
    private var showImageUrl: String? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })

        viewModelScope.launch {
            try {
                coroutineScope {
                    // The show endpoint is cached server-side for an hour, so fetching it again
                    // here (in addition to the detail screen) just for a title/image to report
                    // progress against is cheap - the player doesn't otherwise carry this data.
                    val showDeferred = async { runCatching { api.show(slug) }.getOrNull() }
                    sources = api.playback(slug).sources.toPlaybackSources()
                    showDeferred.await()?.let {
                        showTitle = it.title
                        showImageUrl = it.imageUrl
                    }
                }
                if (sources.isEmpty()) {
                    _state.value = MoviePlayerState.Error("No direct playback source for this title.")
                    return@launch
                }
                selectedIndex = 0
                activateSource(selectedIndex)
            } catch (error: Exception) {
                _state.value = MoviePlayerState.Error(error.message ?: "Unknown error")
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
        val title = showTitle ?: return
        // A WEB-kind source has no real ExoPlayer position/duration to report.
        if (sources.getOrNull(selectedIndex)?.kind != PlaybackSourceKind.DIRECT) return
        val durationMs = player.duration
        if (durationMs <= 0) return
        val positionSeconds = player.currentPosition / 1000.0
        val durationSeconds = durationMs / 1000.0
        viewModelScope.launch {
            mediaAccountRepository.reportProgress(
                MediaProgressRequest(
                    mediaType = MediaAccountType.MOVIE,
                    contentId = slug,
                    title = title,
                    imageUrl = showImageUrl,
                    unitId = slug,
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
        activateSource(index, resume = true)
    }

    /** Re-tries the current source after every source has failed once - see [failedIndices]. */
    fun retryCurrentSource() {
        failedIndices.remove(selectedIndex)
        activateSource(selectedIndex)
    }

    private fun handlePlaybackError(error: PlaybackException) {
        failedIndices.add(selectedIndex)
        val nextIndex = sources.indices.firstOrNull { it !in failedIndices }
        if (nextIndex != null) {
            selectedIndex = nextIndex
            activateSource(nextIndex)
        } else {
            _state.value = MoviePlayerState.Ready(
                player = player,
                sources = sources,
                selectedIndex = selectedIndex,
                playbackError = error.message ?: "Every available source failed to play.",
            )
        }
    }

    /** A WEB-kind source has no playable URL for ExoPlayer - the screen renders it in a WebView
     * instead, so this just pauses the (idle) player and publishes the selection; only a
     * DIRECT-kind source actually touches ExoPlayer, in [playSource]. */
    private fun activateSource(index: Int, resume: Boolean = false) {
        when (sources[index].kind) {
            PlaybackSourceKind.DIRECT -> playSource(index, resume)
            PlaybackSourceKind.WEB -> {
                player.pause()
                _state.value = MoviePlayerState.Ready(player, sources, selectedIndex)
            }
        }
    }

    private fun playSource(index: Int, resume: Boolean = false) {
        val position = if (resume) player.currentPosition else 0L
        val wasPlaying = player.playWhenReady
        player.setMediaItem(sources[index].toMediaItem(), position)
        player.prepare()
        player.playWhenReady = if (resume) wasPlaying else true
        _state.value = MoviePlayerState.Ready(player, sources, selectedIndex)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
