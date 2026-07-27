package cloud.cyberverse.asterion.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.data.remote.FootballApiService
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.ui.anime.currentAnimeSeason
import cloud.cyberverse.asterion.ui.novels.featuredNovels
import cloud.cyberverse.asterion.ui.novels.trendingNovels
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = true,
    val seasonLabel: String = "",
    val seasonalAnime: List<HomeCatalogItem> = emptyList(),
    val freshItems: List<HomeCatalogItem> = emptyList(),
    val footballMatches: List<FootballMatch> = emptyList(),
)

data class HomeSearchState(
    val items: List<HomeCatalogItem> = emptyList(),
    val footballMatches: List<FootballMatch> = emptyList(),
    val isLoading: Boolean = false,
)

private const val SEARCH_DEBOUNCE_MS = 400L

class HomeViewModel(
    private val animeApi: AnimeApiService,
    private val movieApi: MovieApiService,
    private val footballApi: FootballApiService,
    private val novelsApi: AsterionApiService,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow(HomeSearchState())
    val searchState: StateFlow<HomeSearchState> = _searchState.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        val trimmed = newQuery.trim()
        if (trimmed.isEmpty()) {
            _searchState.value = HomeSearchState()
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            search(trimmed)
        }
    }

    private suspend fun search(query: String) = coroutineScope {
        _searchState.value = _searchState.value.copy(isLoading = true)
        val animeDeferred = async { runCatching { animeApi.search(query) }.getOrDefault(emptyList()) }
        val movieDeferred = async { runCatching { movieApi.search(query) }.getOrDefault(emptyList()) }
        val novelDeferred = async { runCatching { novelsApi.novels(search = query, limit = 30) }.getOrNull()?.data.orEmpty() }

        val items = interleave(
            animeDeferred.await().map(HomeCatalogItem::AnimeItem),
            movieDeferred.await().map(HomeCatalogItem::MovieItem),
            novelDeferred.await().map(HomeCatalogItem::NovelItem),
        )
        val matchingFootball = _state.value.footballMatches.filter {
            it.displayTitle.contains(query, ignoreCase = true)
        }
        _searchState.value = HomeSearchState(items = items, footballMatches = matchingFootball, isLoading = false)
    }

    private fun load() {
        viewModelScope.launch {
            val (seasonName, seasonYear) = currentAnimeSeason()
            val seasonLabel = "${seasonName.replaceFirstChar { it.uppercase() }} $seasonYear"

            coroutineScope {
                val seasonalDeferred = async { runCatching { animeApi.season(seasonName, seasonYear) }.getOrDefault(emptyList()) }
                val animeDeferred = async { runCatching { animeApi.popular() }.getOrDefault(emptyList()) }
                val movieDeferred = async { runCatching { movieApi.popular() }.getOrDefault(emptyList()) }
                val novelDeferred = async { runCatching { novelsApi.novels(limit = 60) }.getOrNull()?.data.orEmpty() }
                val footballDeferred = async { runCatching { footballApi.schedule().data }.getOrDefault(emptyList()) }

                val seasonal = seasonalDeferred.await()
                val anime = animeDeferred.await()
                val movies = movieDeferred.await()
                val novels = novelDeferred.await()
                val matches = footballDeferred.await()

                val featured = featuredNovels(novels)
                val trending = trendingNovels(novels).filter { novel -> featured.none { it.id == novel.id } }
                val novelPicks = (featured + trending).distinctBy { it.id }

                _state.value = HomeState(
                    isLoading = false,
                    seasonLabel = seasonLabel,
                    seasonalAnime = seasonal.map(HomeCatalogItem::AnimeItem),
                    freshItems = interleave(
                        anime.take(8).map(HomeCatalogItem::AnimeItem),
                        movies.take(8).map(HomeCatalogItem::MovieItem),
                        novelPicks.take(8).map(HomeCatalogItem::NovelItem),
                    ),
                    footballMatches = upcomingAndLive(matches),
                )
            }
        }
    }
}

/** Live matches first, then upcoming kickoffs soonest-first - mirrors AsterionMac's footballMatches. */
private fun upcomingAndLive(matches: List<FootballMatch>): List<FootballMatch> {
    val now = System.currentTimeMillis()
    return matches
        .filter { it.isLive || it.kickoffMillis >= now }
        .sortedWith(compareByDescending<FootballMatch> { it.isLive }.thenBy { it.kickoffMillis })
}
