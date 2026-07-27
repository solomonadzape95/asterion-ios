package cloud.cyberverse.asterion.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.MediaAccountType
import cloud.cyberverse.asterion.data.model.MovieShow
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MovieDetailState {
    data object Loading : MovieDetailState
    data class Loaded(
        val show: MovieShow,
        val isBookmarked: Boolean = false,
        val isBookmarkUpdating: Boolean = false,
    ) : MovieDetailState
    data class Error(val message: String) : MovieDetailState
}

class MovieDetailViewModel(
    private val api: MovieApiService,
    private val mediaAccountRepository: MediaAccountRepository,
    private val slug: String,
) : ViewModel() {
    private val _state = MutableStateFlow<MovieDetailState>(MovieDetailState.Loading)
    val state: StateFlow<MovieDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val show = api.show(slug)
                // The snapshot is shared app-wide (see MediaAccountRepository) - only refresh it
                // if nothing's loaded it yet, so opening several detail screens in a row doesn't
                // re-fetch the whole account snapshot each time.
                if (mediaAccountRepository.snapshot.value == null) mediaAccountRepository.refresh()
                MovieDetailState.Loaded(show, mediaAccountRepository.isBookmarked(MediaAccountType.MOVIE, slug))
            } catch (error: Exception) {
                MovieDetailState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun toggleBookmark() {
        val current = _state.value as? MovieDetailState.Loaded ?: return
        if (current.isBookmarkUpdating) return
        _state.value = current.copy(isBookmarkUpdating = true)
        viewModelScope.launch {
            val nowBookmarked = mediaAccountRepository.toggleBookmark(
                mediaType = MediaAccountType.MOVIE,
                contentId = slug,
                title = current.show.title,
                subtitle = if (current.show.isSeries) "TV Series" else "Movie",
                imageUrl = current.show.imageUrl,
            )
            val latest = _state.value as? MovieDetailState.Loaded ?: return@launch
            _state.value = latest.copy(isBookmarked = nowBookmarked, isBookmarkUpdating = false)
        }
    }
}
