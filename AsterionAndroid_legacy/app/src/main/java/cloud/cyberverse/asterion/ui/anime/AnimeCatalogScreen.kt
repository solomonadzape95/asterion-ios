package cloud.cyberverse.asterion.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.cyberverse.asterion.data.model.AnimeTitle
import cloud.cyberverse.asterion.ui.components.PhosphorIcons
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.BackToTopButton
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.AsterionSearchField
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.AsterionWordmark
import cloud.cyberverse.asterion.ui.components.CoverCard
import cloud.cyberverse.asterion.ui.components.EmptyState
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.ui.components.SectionHeader
import cloud.cyberverse.asterion.ui.common.SearchUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val AUTO_ADVANCE_DELAY_MS = 4500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeCatalogScreen(onTitleClick: (AnimeTitle) -> Unit, viewModel: AnimeCatalogViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val seasonal by viewModel.seasonal.collectAsState()
    val discover by viewModel.discover.collectAsState()
    val query by viewModel.query.collectAsState()
    val isSearching = query.isNotBlank()

    Scaffold(topBar = { AsterionTopBar(title = { AsterionWordmark() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AsterionSearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Search anime…",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (isSearching) {
                when (val current = searchState) {
                    is SearchUiState.Idle, is SearchUiState.Loading -> AsterionLoadingBox()

                    is SearchUiState.Error -> ErrorState(
                        message = current.message,
                        onRetry = viewModel::retrySearch,
                    )

                    is SearchUiState.Loaded -> if (current.results.isEmpty()) {
                        EmptyState("No anime match “$query”")
                    } else {
                        TitleGrid(
                            current.results,
                            header = "Search Results" to "Titles matching your search.",
                            onTitleClick = onTitleClick,
                        )
                    }
                }
            } else {
                when (val current = state) {
                    is AnimeCatalogState.Loading -> AsterionLoadingBox()

                    // Only take over the screen when there is genuinely nothing to show. If the
                    // discover grid loaded, a failed "recently updated" call shouldn't blank it.
                    is AnimeCatalogState.Error -> if (discover.titles.isEmpty()) {
                        ErrorState(message = current.message, onRetry = viewModel::load)
                    } else {
                        AnimeDiscoverScreen(
                            recentlyUpdated = emptyList(),
                            seasonal = seasonal,
                            seasonLabel = viewModel.seasonLabel,
                            discover = discover,
                            onLoadMore = viewModel::loadMoreDiscover,
                            onTitleClick = onTitleClick,
                        )
                    }

                    is AnimeCatalogState.Loaded -> if (current.titles.isEmpty() && discover.titles.isEmpty()) {
                        EmptyState("No anime found")
                    } else {
                        AnimeDiscoverScreen(
                            recentlyUpdated = current.titles,
                            seasonal = seasonal,
                            seasonLabel = viewModel.seasonLabel,
                            discover = discover,
                            onLoadMore = viewModel::loadMoreDiscover,
                            onTitleClick = onTitleClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeDiscoverScreen(
    recentlyUpdated: List<AnimeTitle>,
    seasonal: AnimeSeasonalState,
    seasonLabel: String,
    discover: AnimeDiscoverState,
    onLoadMore: () -> Unit,
    onTitleClick: (AnimeTitle) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(gridState, discover.titles.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (lastVisible >= gridState.layoutInfo.totalItemsCount - 6) {
                    onLoadMore()
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimeFeaturedBanner(titles = recentlyUpdated.take(8), onTitleClick = onTitleClick)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SeasonalShelf(seasonal = seasonal, seasonLabel = seasonLabel, onTitleClick = onTitleClick)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimeShelf(
                    title = "Recently Updated",
                    subtitle = "Fresh episodes, ready when you are.",
                    titles = recentlyUpdated,
                    onTitleClick = onTitleClick,
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Discover",
                    subtitle = "Explore the full anime catalog.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(discover.titles, key = { it.slug }) { title ->
                CoverCard(
                    imageUrl = title.imageUrl,
                    title = title.title,
                    subtitle = title.episodeLabel,
                    width = 120.dp,
                    onClick = { onTitleClick(title) },
                    modifier = Modifier.padding(8.dp),
                )
            }
            if (discover.isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        AsterionLoadingIndicator()
                    }
                }
            }
        }

        BackToTopButton(
            visible = gridState.firstVisibleItemIndex > 4,
            onClick = { scope.launch { gridState.animateScrollToItem(0) } },
        )
    }
}

@Composable
private fun AnimeFeaturedBanner(titles: List<AnimeTitle>, onTitleClick: (AnimeTitle) -> Unit) {
    if (titles.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, titles.size) {
        delay(AUTO_ADVANCE_DELAY_MS)
        if (!pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % titles.size)
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val featured = titles[page]
            Box(Modifier.fillMaxSize()) {
                AsterionAsyncImage(
                    model = featured.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(26.dp),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.32f)),
                        ),
                    ),
                )
                Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            "FEATURED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            featured.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            featured.episodeLabel?.let { BannerBadge(it, MaterialTheme.colorScheme.primary) }
                            featured.type?.let { BannerBadge(it, Color.White.copy(alpha = 0.18f)) }
                        }
                        Button(
                            onClick = { onTitleClick(featured) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(top = 14.dp),
                        ) {
                            Icon(PhosphorIcons.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Watch now")
                        }
                    }
                    AsterionAsyncImage(
                        model = featured.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { onTitleClick(featured) }
                            .width(88.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            titles.indices.forEach { dotIndex ->
                val isSelected = dotIndex == pagerState.currentPage
                Box(
                    Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f))
                        .clickable { scope.launch { pagerState.animateScrollToPage(dotIndex) } },
                )
            }
        }
    }
}

@Composable
private fun BannerBadge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun SeasonalShelf(seasonal: AnimeSeasonalState, seasonLabel: String, onTitleClick: (AnimeTitle) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        SectionHeader(
            title = seasonLabel,
            subtitle = "Anime airing in the current season.",
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        when (seasonal) {
            is AnimeSeasonalState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp)) { AsterionLoadingIndicator() }
            is AnimeSeasonalState.Error -> Text(
                "The anime service couldn't load this season right now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            is AnimeSeasonalState.Loaded -> if (seasonal.titles.isEmpty()) {
                Text(
                    "No titles listed for $seasonLabel yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                TitleRow(seasonal.titles, onTitleClick)
            }
        }
    }
}

@Composable
private fun AnimeShelf(
    title: String,
    subtitle: String,
    titles: List<AnimeTitle>,
    onTitleClick: (AnimeTitle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(title = title, subtitle = subtitle, modifier = Modifier.padding(horizontal = 20.dp))
        TitleRow(titles, onTitleClick)
    }
}

@Composable
private fun TitleRow(titles: List<AnimeTitle>, onTitleClick: (AnimeTitle) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        items(titles, key = { it.slug }) { title ->
            CoverCard(
                imageUrl = title.imageUrl,
                title = title.title,
                subtitle = title.type ?: "Anime",
                width = 128.dp,
                topEndBadge = title.episodeLabel,
                onClick = { onTitleClick(title) },
            )
        }
    }
}

@Composable
private fun TitleGrid(titles: List<AnimeTitle>, header: Pair<String, String>, onTitleClick: (AnimeTitle) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = header.first, subtitle = header.second, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        }
        items(titles, key = { it.slug }) { title ->
            CoverCard(
                imageUrl = title.imageUrl,
                title = title.title,
                subtitle = title.episodeLabel,
                width = 120.dp,
                onClick = { onTitleClick(title) },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
