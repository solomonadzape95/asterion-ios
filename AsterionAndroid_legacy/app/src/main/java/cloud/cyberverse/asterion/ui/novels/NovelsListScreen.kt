package cloud.cyberverse.asterion.ui.novels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.AsterionSearchField
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.AsterionWordmark
import cloud.cyberverse.asterion.ui.components.BackToTopButton
import cloud.cyberverse.asterion.ui.components.FastScrollbar
import cloud.cyberverse.asterion.ui.components.CoverCard
import cloud.cyberverse.asterion.ui.components.EmptyState
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.ui.components.SectionHeader
import cloud.cyberverse.asterion.ui.components.SectionTabRow
import cloud.cyberverse.asterion.ui.common.SearchUiState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelsListScreen(onNovelClick: (Novel) -> Unit, viewModel: NovelsListViewModel = koinViewModel()) {
    val discover by viewModel.discover.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val query by viewModel.query.collectAsState()
    val section by viewModel.section.collectAsState()
    val isSearching = query.isNotBlank()

    Scaffold(topBar = { AsterionTopBar(title = { AsterionWordmark() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AsterionSearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Search titles, authors, or genres",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (!isSearching) {
                SectionTabRow(
                    sections = NovelSection.entries,
                    selected = section,
                    onSelect = viewModel::onSectionChange,
                    label = { it.title },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            when {
                isSearching -> when (val current = searchState) {
                    is SearchUiState.Idle, is SearchUiState.Loading -> AsterionLoadingBox()
                    is SearchUiState.Error -> ErrorState(
                        message = current.message,
                        onRetry = viewModel::retrySearch,
                    )
                    is SearchUiState.Loaded -> if (current.results.isEmpty()) {
                        EmptyState("No novels match “$query”")
                    } else {
                        NovelGrid(
                            novels = current.results,
                            header = "Search Results" to "Titles matching your search.",
                            onNovelClick = onNovelClick,
                        )
                    }
                }

                discover.isLoading -> AsterionLoadingBox()

                discover.novels.isEmpty() && discover.error != null -> ErrorState(
                    message = discover.error!!,
                    onRetry = viewModel::loadMoreDiscover,
                )

                else -> when (section) {
                    NovelSection.Discover -> DiscoverShelves(discover.novels, onNovelClick)
                    NovelSection.Rankings -> RankingsGrid(
                        novels = rankedNovels(discover.novels),
                        isLoadingMore = discover.isLoadingMore,
                        canLoadMore = discover.canLoadMore,
                        onLoadMore = viewModel::loadMoreDiscover,
                        onNovelClick = onNovelClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverShelves(novels: List<Novel>, onNovelClick: (Novel) -> Unit) {
    val featured = remember(novels) { featuredNovels(novels) }
    val trending = remember(novels) { trendingNovels(novels) }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "featured") {
            NovelShelf(
                title = "Featured",
                subtitle = "Handpicked stories worth your time.",
                novels = featured,
                onNovelClick = onNovelClick,
            )
        }
        item(key = "trending") {
            NovelShelf(
                title = "Trending This Week",
                subtitle = "Stories readers keep returning to.",
                novels = trending,
                onNovelClick = onNovelClick,
                modifier = Modifier.padding(top = 28.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun NovelShelf(
    title: String,
    subtitle: String,
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(title = title, subtitle = subtitle, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            items(novels, key = { it.id }) { novel ->
                CoverCard(
                    imageUrl = novel.imageUrl,
                    title = novel.title,
                    subtitle = novel.author,
                    width = 128.dp,
                    onClick = { onNovelClick(novel) },
                )
            }
        }
    }
}

/** The full catalog as an infinite-scroll grid - loads the next page automatically as you
 * approach the bottom, same pattern as the Movies/Anime discover grids. */
@Composable
private fun RankingsGrid(
    novels: List<Novel>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onNovelClick: (Novel) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(gridState, novels.size, canLoadMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (canLoadMore && lastVisible >= gridState.layoutInfo.totalItemsCount - 6) {
                    onLoadMore()
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Rankings",
                    subtitle = "The most-loved stories in Asterion.",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            items(novels, key = { it.id }) { novel ->
                CoverCard(
                    imageUrl = novel.imageUrl,
                    title = novel.title,
                    subtitle = novel.author,
                    width = 120.dp,
                    topStartBadge = if (novel.numericRank != Int.MAX_VALUE) "#${novel.numericRank}" else null,
                    onClick = { onNovelClick(novel) },
                    modifier = Modifier.padding(8.dp),
                )
            }
            if (isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        AsterionLoadingIndicator()
                    }
                }
            }
        }

        FastScrollbar(
            state = gridState,
            totalItems = novels.size,
        )

        BackToTopButton(
            visible = gridState.firstVisibleItemIndex > 4,
            onClick = { scope.launch { gridState.animateScrollToItem(0) } },
        )
    }
}

@Composable
private fun NovelGrid(
    novels: List<Novel>,
    header: Pair<String, String>,
    onNovelClick: (Novel) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = header.first, subtitle = header.second, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        }
        items(novels, key = { it.id }) { novel ->
            CoverCard(
                imageUrl = novel.imageUrl,
                title = novel.title,
                subtitle = novel.author,
                width = 120.dp,
                onClick = { onNovelClick(novel) },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
