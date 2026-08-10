package cloud.cyberverse.asterion.ui.novels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.ui.common.SearchUiState
import cloud.cyberverse.asterion.ui.common.launchSearch
import cloud.cyberverse.asterion.ui.common.userMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NovelSection(val title: String) {
    Discover("Discover"),
    Rankings("Rankings"),
}

/** Grows one page at a time as the Rankings grid is scrolled, instead of blocking on the whole
 * catalog up front - the old fetchAllNovels loop made every visit wait through 150+ novels' worth
 * of pages before anything rendered at all. */
data class NovelDiscoverState(
    val novels: List<Novel> = emptyList(),
    val offset: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
)

/** Mirrors AsterionMac's Novel.numericRank: strip non-digits, unranked sorts last. */
val Novel.numericRank: Int
    get() = rank?.filter { it.isDigit() }?.toIntOrNull() ?: Int.MAX_VALUE

private const val SEARCH_DEBOUNCE_MS = 400L
private const val DISCOVER_PAGE_SIZE = 40

class NovelsListViewModel(private val api: AsterionApiService) : ViewModel() {
    private val _discover = MutableStateFlow(NovelDiscoverState())
    val discover: StateFlow<NovelDiscoverState> = _discover.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _section = MutableStateFlow(NovelSection.Discover)
    val section: StateFlow<NovelSection> = _section.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState<Novel>>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState<Novel>> = _searchState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadMoreDiscover()
    }

    fun onSectionChange(newSection: NovelSection) {
        _section.value = newSection
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        searchJob = launchSearch(_searchState, newQuery) { fetchAllNovels(it) }
    }

    fun retrySearch() {
        searchJob?.cancel()
        searchJob = launchSearch(_searchState, _query.value, debounceMs = 0L) { fetchAllNovels(it) }
    }

    /** Loads the next page of the full catalog, appending to whatever's already loaded - Featured
     * and Trending (sorted by rank/rating over whatever's loaded so far) refine as more pages come
     * in rather than waiting for everything. */
    fun loadMoreDiscover() {
        val current = _discover.value
        if (current.isLoadingMore || !current.canLoadMore) return
        _discover.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            _discover.value = try {
                val page = api.novels(limit = DISCOVER_PAGE_SIZE, offset = current.offset)
                val seenIds = current.novels.mapTo(mutableSetOf()) { it.id }
                val newNovels = page.data.filter { seenIds.add(it.id) }
                val merged = current.novels + newNovels
                val total = page.meta?.total ?: merged.size
                current.copy(
                    novels = merged,
                    offset = current.offset + DISCOVER_PAGE_SIZE,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = newNovels.isNotEmpty() && merged.size < total,
                    error = null,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                current.copy(isLoading = false, isLoadingMore = false, error = error.userMessage())
            }
        }
    }

    /**
     * /novels defaults to a 25-item page; with 150+ novels in the catalog, only fetching page 1
     * silently truncated search results to whatever arbitrary slice happened to load first.
     * Mirrors AsterionMac's APIClient.fetchAllNovels: page with a large limit until the
     * server-reported total is exhausted. Search result sets are small enough that doing this
     * exhaustively (rather than incrementally, like [loadMoreDiscover]) is fine.
     */
    private suspend fun fetchAllNovels(search: String?): List<Novel> {
        val pageSize = 100
        var offset = 0
        val result = mutableListOf<Novel>()
        val seenIds = mutableSetOf<String>()
        while (true) {
            val page = api.novels(search = search, limit = pageSize, offset = offset)
            val newNovels = page.data.filter { seenIds.add(it.id) }
            result += newNovels
            val total = page.meta?.total ?: page.data.size
            offset += pageSize
            if (newNovels.isEmpty() || result.size >= total || page.data.size < pageSize) break
        }
        return result
    }
}

/** Top 4 by rank (rating breaks ties) - AsterionMac's AppModel.featuredNovels. */
fun featuredNovels(novels: List<Novel>): List<Novel> = novels
    .sortedWith(
        compareBy<Novel> { it.numericRank }.thenByDescending { it.rating ?: 0.0 },
    )
    .take(4)

/** Top 8 by rating (rank breaks ties) - AsterionMac's AppModel.trendingNovels. */
fun trendingNovels(novels: List<Novel>): List<Novel> = novels
    .sortedWith(
        compareByDescending<Novel> { it.rating ?: 0.0 }.thenBy { it.numericRank },
    )
    .take(8)

/** Full catalog ordered by rank - AsterionMac's novels(for: .rankings). */
fun rankedNovels(novels: List<Novel>): List<Novel> = novels
    .sortedWith(compareBy<Novel> { it.numericRank }.thenBy { it.title })
