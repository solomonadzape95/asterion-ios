package cloud.cyberverse.asterion.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.cyberverse.asterion.data.model.AnimeTitle
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.data.model.FootballTeam
import cloud.cyberverse.asterion.data.model.MovieTitle
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionSearchField
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.AsterionWordmark
import cloud.cyberverse.asterion.ui.components.CoverCard
import cloud.cyberverse.asterion.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AUTO_ADVANCE_DELAY_MS = 5000L

@Composable
fun HomeScreen(
    onAnimeClick: (AnimeTitle) -> Unit,
    onMovieClick: (MovieTitle) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onFootballClick: (FootballMatch) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val isSearching = query.isNotBlank()

    fun openItem(item: HomeCatalogItem) {
        when (item) {
            is HomeCatalogItem.AnimeItem -> onAnimeClick(item.anime)
            is HomeCatalogItem.MovieItem -> onMovieClick(item.movie)
            is HomeCatalogItem.NovelItem -> onNovelClick(item.novel)
        }
    }

    Scaffold(topBar = { AsterionTopBar(title = { AsterionWordmark() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AsterionSearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Search across Asterion…",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                isSearching -> HomeSearchResults(
                    query = query,
                    searchState = searchState,
                    onItemClick = ::openItem,
                    onFootballClick = onFootballClick,
                )
                state.isLoading -> AsterionLoadingBox()
                else -> HomeDashboard(state = state, onItemClick = ::openItem, onFootballClick = onFootballClick)
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    state: HomeState,
    onItemClick: (HomeCatalogItem) -> Unit,
    onFootballClick: (FootballMatch) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (state.freshItems.isNotEmpty()) {
            item(key = "featured") {
                HomeFeaturedBanner(items = state.freshItems.take(8), onItemClick = onItemClick)
            }
        }

        if (state.footballMatches.isNotEmpty()) {
            item(key = "football-header") {
                SectionHeader(
                    title = "Football",
                    subtitle = "Live and upcoming matches.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item(key = "football-shelf") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    items(state.footballMatches.take(12), key = { it.id }) { match ->
                        HomeMatchCard(match = match, onClick = { onFootballClick(match) })
                    }
                }
            }
        }

        if (state.seasonalAnime.isNotEmpty()) {
            item(key = "seasonal-header") {
                SectionHeader(
                    title = state.seasonLabel,
                    subtitle = "This season's anime.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item(key = "seasonal-shelf") {
                HomePosterShelf(items = state.seasonalAnime, onItemClick = onItemClick)
            }
        }

        if (state.freshItems.isNotEmpty()) {
            item(key = "fresh-header") {
                SectionHeader(
                    title = "Fresh across Asterion",
                    subtitle = "New anime, movies, and novels.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item(key = "fresh-shelf") {
                HomePosterShelf(items = state.freshItems, onItemClick = onItemClick)
            }
        }
    }
}

@Composable
private fun HomePosterShelf(items: List<HomeCatalogItem>, onItemClick: (HomeCatalogItem) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        items(items, key = { it.title + it.badge }) { item ->
            CoverCard(
                imageUrl = item.imageUrl,
                title = item.title,
                subtitle = item.subtitle,
                width = 128.dp,
                topStartBadge = item.badge,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun HomeFeaturedBanner(items: List<HomeCatalogItem>, onItemClick: (HomeCatalogItem) -> Unit) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // See MovieCatalogScreen's MovieFeaturedBanner for why this can't key on currentPage.
    LaunchedEffect(items.size) {
        while (true) {
            delay(AUTO_ADVANCE_DELAY_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
            }
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
            val featured = items[page]
            Box(Modifier.fillMaxSize()) {
                AsterionAsyncImage(
                    model = featured.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(26.dp),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.28f)),
                        ),
                    ),
                )
                Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            "FEATURED ACROSS ASTERION",
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
                            FeaturedBadge(featured.badge)
                            if (featured.subtitle.isNotBlank()) FeaturedBadge(featured.subtitle)
                        }
                        Button(
                            onClick = { onItemClick(featured) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(top = 14.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Open")
                        }
                    }
                    AsterionAsyncImage(
                        model = featured.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { onItemClick(featured) }
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
            items.indices.forEach { dotIndex ->
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
private fun FeaturedBadge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun HomeMatchCard(match: FootballMatch, onClick: () -> Unit) {
    val kickoffFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        if (match.isLive) {
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        } else {
            Text(
                kickoffFormatter.format(Date(match.kickoffMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            TeamBadge(match.teams?.home)
            Text(
                "vs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            TeamBadge(match.teams?.away)
        }

        Text(
            match.displayTitle,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun TeamBadge(team: FootballTeam?) {
    if (team?.badgeUrl != null) {
        AsterionAsyncImage(
            model = team.badgeUrl,
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(CircleShape),
        )
    } else {
        Icon(
            Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun HomeSearchResults(
    query: String,
    searchState: HomeSearchState,
    onItemClick: (HomeCatalogItem) -> Unit,
    onFootballClick: (FootballMatch) -> Unit,
) {
    if (searchState.isLoading && searchState.items.isEmpty() && searchState.footballMatches.isEmpty()) {
        AsterionLoadingBox()
        return
    }

    if (searchState.items.isEmpty() && searchState.footballMatches.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results match \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (searchState.items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Search results",
                    subtitle = "Across novels, anime, movies, and TV shows.",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            items(searchState.items, key = { it.title + it.badge }) { item ->
                CoverCard(
                    imageUrl = item.imageUrl,
                    title = item.title,
                    subtitle = item.subtitle,
                    width = 120.dp,
                    topStartBadge = item.badge,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        if (searchState.footballMatches.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Football",
                    subtitle = "Matching fixtures.",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(searchState.footballMatches, key = { it.id }) { match ->
                        HomeMatchCard(match = match, onClick = { onFootballClick(match) })
                    }
                }
            }
        }
    }
}
