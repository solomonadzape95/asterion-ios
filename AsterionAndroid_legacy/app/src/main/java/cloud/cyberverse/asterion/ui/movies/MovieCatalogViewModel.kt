package cloud.cyberverse.asterion.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.MovieTitle
import cloud.cyberverse.asterion.data.remote.MovieApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MovieCatalogState {
    data object Loading : MovieCatalogState
    data class Loaded(val titles: List<MovieTitle>) : MovieCatalogState
    data class Error(val message: String) : MovieCatalogState
}

data class MovieDiscoverState(
    val titles: List<MovieTitle> = emptyList(),
    val page: Int = 0,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
)

private const val SEARCH_DEBOUNCE_MS = 400L

class MovieCatalogViewModel(private val api: MovieApiService) : ViewModel() {
    private val _state = MutableStateFlow<MovieCatalogState>(MovieCatalogState.Loading)
    val state: StateFlow<MovieCatalogState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _discover = MutableStateFlow(MovieDiscoverState())
    val discover: StateFlow<MovieDiscoverState> = _discover.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
        loadMoreDiscover()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(newQuery)
        }
    }

    private fun load(search: String = "") {
        _state.value = MovieCatalogState.Loading
        viewModelScope.launch {
            _state.value = try {
                val titles = if (search.isBlank()) api.popular() else api.search(search.trim())
                MovieCatalogState.Loaded(titles)
            } catch (error: Exception) {
                MovieCatalogState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun loadMoreDiscover() {
        val current = _discover.value
        if (current.isLoadingMore || !current.canLoadMore) return
        _discover.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            val nextPage = current.page + 1
            _discover.value = try {
                val newTitles = api.popular(nextPage)
                current.copy(
                    titles = current.titles + newTitles,
                    page = nextPage,
                    isLoadingMore = false,
                    canLoadMore = newTitles.isNotEmpty(),
                )
            } catch (error: Exception) {
                current.copy(isLoadingMore = false, canLoadMore = false)
            }
        }
    }
}
