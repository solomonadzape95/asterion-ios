package cloud.cyberverse.asterion.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.MovieTitle
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.ui.common.SearchUiState
import cloud.cyberverse.asterion.ui.common.launchSearch
import cloud.cyberverse.asterion.ui.common.userMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
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

class MovieCatalogViewModel(private val api: MovieApiService) : ViewModel() {
    private val _state = MutableStateFlow<MovieCatalogState>(MovieCatalogState.Loading)
    val state: StateFlow<MovieCatalogState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Separate from [state] so a failed search can't blank the trending row behind it. */
    private val _searchState = MutableStateFlow<SearchUiState<MovieTitle>>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState<MovieTitle>> = _searchState.asStateFlow()

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
        searchJob = launchSearch(_searchState, newQuery) { api.search(it) }
    }

    fun retrySearch() {
        searchJob?.cancel()
        searchJob = launchSearch(_searchState, _query.value, debounceMs = 0L) { api.search(it) }
    }

    fun load() {
        _state.value = MovieCatalogState.Loading
        viewModelScope.launch {
            _state.value = try {
                MovieCatalogState.Loaded(api.popular())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                MovieCatalogState.Error(error.userMessage())
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
                val page = api.catalog(nextPage)
                val seenIds = current.titles.mapTo(mutableSetOf()) { it.id }
                val newTitles = page.results.filter { seenIds.add(it.id) }
                current.copy(
                    titles = current.titles + newTitles,
                    page = nextPage,
                    isLoadingMore = false,
                    canLoadMore = newTitles.isNotEmpty() && page.page < page.totalPages,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                current.copy(isLoadingMore = false, canLoadMore = false)
            }
        }
    }
}
