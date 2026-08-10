package cloud.cyberverse.asterion.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.AnimeTitle
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.ui.common.SearchUiState
import cloud.cyberverse.asterion.ui.common.launchSearch
import cloud.cyberverse.asterion.ui.common.userMessage
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnimeCatalogState {
    data object Loading : AnimeCatalogState
    data class Loaded(val titles: List<AnimeTitle>) : AnimeCatalogState
    data class Error(val message: String) : AnimeCatalogState
}

sealed interface AnimeSeasonalState {
    data object Loading : AnimeSeasonalState
    data class Loaded(val titles: List<AnimeTitle>) : AnimeSeasonalState
    data class Error(val message: String) : AnimeSeasonalState
}

data class AnimeDiscoverState(
    val titles: List<AnimeTitle> = emptyList(),
    val page: Int = 0,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
)

/** AsterionMac's AnimeSeason.current(): Jan-Mar winter, Apr-Jun spring, Jul-Sep summer, else fall. */
fun currentAnimeSeason(): Pair<String, Int> {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH) + 1
    val name = when (month) {
        in 1..3 -> "winter"
        in 4..6 -> "spring"
        in 7..9 -> "summer"
        else -> "fall"
    }
    return name to calendar.get(Calendar.YEAR)
}

class AnimeCatalogViewModel(private val api: AnimeApiService) : ViewModel() {
    private val _state = MutableStateFlow<AnimeCatalogState>(AnimeCatalogState.Loading)
    val state: StateFlow<AnimeCatalogState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Separate from [state] so a failed search can't blank the seasonal shelf behind it. */
    private val _searchState = MutableStateFlow<SearchUiState<AnimeTitle>>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState<AnimeTitle>> = _searchState.asStateFlow()

    private val _seasonal = MutableStateFlow<AnimeSeasonalState>(AnimeSeasonalState.Loading)
    val seasonal: StateFlow<AnimeSeasonalState> = _seasonal.asStateFlow()

    private val _discover = MutableStateFlow(AnimeDiscoverState())
    val discover: StateFlow<AnimeDiscoverState> = _discover.asStateFlow()

    val seasonLabel: String
    private val seasonName: String
    private val seasonYear: Int

    private var searchJob: Job? = null

    init {
        val (name, year) = currentAnimeSeason()
        seasonName = name
        seasonYear = year
        seasonLabel = "${name.replaceFirstChar { it.uppercase() }} $year"
        load()
        loadSeasonal()
        loadMoreDiscover()
    }

    fun loadMoreDiscover() {
        val current = _discover.value
        if (current.isLoadingMore || !current.canLoadMore) return
        _discover.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            val nextPage = current.page + 1
            _discover.value = try {
                val seenSlugs = current.titles.mapTo(mutableSetOf()) { it.slug }
                val newTitles = api.popular(nextPage).filter { seenSlugs.add(it.slug) }
                current.copy(
                    titles = current.titles + newTitles,
                    page = nextPage,
                    isLoadingMore = false,
                    canLoadMore = newTitles.isNotEmpty(),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                current.copy(isLoadingMore = false, canLoadMore = false)
            }
        }
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
        _state.value = AnimeCatalogState.Loading
        viewModelScope.launch {
            _state.value = try {
                AnimeCatalogState.Loaded(api.popular())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AnimeCatalogState.Error(error.userMessage())
            }
        }
    }

    private fun loadSeasonal() {
        viewModelScope.launch {
            _seasonal.value = try {
                AnimeSeasonalState.Loaded(api.season(seasonName, seasonYear))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AnimeSeasonalState.Error(error.userMessage())
            }
        }
    }
}
