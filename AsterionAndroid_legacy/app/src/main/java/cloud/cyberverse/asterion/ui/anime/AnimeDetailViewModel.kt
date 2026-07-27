package cloud.cyberverse.asterion.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.AnimeEpisode
import cloud.cyberverse.asterion.data.model.AnimeRelatedSeason
import cloud.cyberverse.asterion.data.model.AnimeShow
import cloud.cyberverse.asterion.data.model.MediaAccountType
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnimeDetailState {
    data object Loading : AnimeDetailState
    data class Loaded(
        val show: AnimeShow,
        val episodes: List<AnimeEpisode>,
        val relatedSeasons: List<AnimeRelatedSeason> = emptyList(),
        val isBookmarked: Boolean = false,
        val isBookmarkUpdating: Boolean = false,
    ) : AnimeDetailState
    data class Error(val message: String) : AnimeDetailState
}

class AnimeDetailViewModel(
    private val api: AnimeApiService,
    private val mediaAccountRepository: MediaAccountRepository,
    initialSlug: String,
) : ViewModel() {
    private val _state = MutableStateFlow<AnimeDetailState>(AnimeDetailState.Loading)
    val state: StateFlow<AnimeDetailState> = _state.asStateFlow()

    init {
        load(initialSlug)
    }

    fun selectSeason(season: AnimeRelatedSeason) {
        load(season.slug)
    }

    fun toggleBookmark() {
        val current = _state.value as? AnimeDetailState.Loaded ?: return
        if (current.isBookmarkUpdating) return
        _state.value = current.copy(isBookmarkUpdating = true)
        viewModelScope.launch {
            val nowBookmarked = mediaAccountRepository.toggleBookmark(
                mediaType = MediaAccountType.ANIME,
                // The show's own id (not the nav slug) - selecting a related season loads a
                // different AnimeShow with its own id, and each season is bookmarked separately.
                contentId = current.show.id,
                title = current.show.title,
                subtitle = current.show.type,
                imageUrl = current.show.imageUrl,
            )
            val latest = _state.value as? AnimeDetailState.Loaded ?: return@launch
            _state.value = latest.copy(isBookmarked = nowBookmarked, isBookmarkUpdating = false)
        }
    }

    private fun load(slug: String) {
        _state.value = AnimeDetailState.Loading
        viewModelScope.launch {
            _state.value = try {
                val show = api.show(slug)
                val episodes = api.episodes(show.id)
                val relatedSeasons = runCatching { api.relatedSeasons(show.id) }.getOrDefault(emptyList())
                if (mediaAccountRepository.snapshot.value == null) mediaAccountRepository.refresh()
                AnimeDetailState.Loaded(
                    show = show,
                    episodes = episodes,
                    relatedSeasons = relatedSeasons,
                    isBookmarked = mediaAccountRepository.isBookmarked(MediaAccountType.ANIME, show.id),
                )
            } catch (error: Exception) {
                AnimeDetailState.Error(error.message ?: "Unknown error")
            }
        }
    }
}
