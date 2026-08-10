package cloud.cyberverse.asterion.ui.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.model.AnimeEpisode
import cloud.cyberverse.asterion.data.model.AnimeShow
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionFilledButton
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.ExpandableSynopsis
import cloud.cyberverse.asterion.ui.components.SectionHeader
import cloud.cyberverse.asterion.ui.downloads.DownloadPlannerSheet
import cloud.cyberverse.asterion.ui.downloads.PlannerUnit
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    slug: String,
    onEpisodeClick: (AnimeShow, AnimeEpisode) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: AnimeDetailViewModel = koinViewModel(parameters = { parametersOf(slug) }),
    animeApi: AnimeApiService = koinInject(),
    videoDownloadManager: VideoDownloadManager = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showPlanner by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val showTitleInBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 500 }
    }

    Scaffold(
        topBar = {
            val loaded = state as? AnimeDetailState.Loaded
            AsterionTopBar(
                title = if (showTitleInBar) loaded?.show?.title else null,
                onBack = onNavigateBack,
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = viewModel::toggleBookmark, enabled = !loaded.isBookmarkUpdating) {
                            Icon(
                                if (loaded.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (loaded.isBookmarked) "Remove from saved" else "Save",
                            )
                        }
                        IconButton(onClick = { showPlanner = true }) {
                            Icon(Icons.Filled.Download, contentDescription = "Download episodes")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is AnimeDetailState.Loading -> AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))

            is AnimeDetailState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
            )

            is AnimeDetailState.Loaded -> {
                val episodeChunkSize = 100
                val episodeChunks = remember(current.episodes) { current.episodes.chunked(episodeChunkSize) }
                var selectedEpisodeChunk by remember(current.episodes) { mutableIntStateOf(0) }

                LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
                item(key = "hero") {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AsterionAsyncImage(
                            model = current.show.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .width(156.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Text(
                            current.show.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                        current.show.studio?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 14.dp)) {
                            current.show.status?.let { MetaText(it) }
                            current.show.subEpisodes?.let { MetaText("$it episodes") }
                            current.show.malScore?.let {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(16.dp))
                                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (current.show.genres.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                current.show.genres.take(4).forEach { genre ->
                                    Text(
                                        genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        AsterionFilledButton(
                            text = "Watch Episode 1",
                            icon = Icons.Filled.PlayCircle,
                            onClick = { current.episodes.firstOrNull()?.let { onEpisodeClick(current.show, it) } },
                            modifier = Modifier.padding(top = 22.dp).fillMaxWidth(),
                        )
                    }
                }
                if (current.relatedSeasons.size > 1) {
                    item(key = "seasons") {
                        Column(Modifier.padding(top = 8.dp)) {
                            SectionHeader(title = "Seasons", modifier = Modifier.padding(horizontal = 20.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                items(current.relatedSeasons, key = { it.id }) { season ->
                                    FilterChip(
                                        selected = season.id == current.show.id,
                                        onClick = { viewModel.selectSeason(season) },
                                        label = { Text(season.title) },
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "synopsis") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Synopsis")
                        ExpandableSynopsis(current.show.description, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                item(key = "episodesHeader") {
                    SectionHeader(
                        title = "Episodes",
                        subtitle = "${current.episodes.size} episodes",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                if (episodeChunks.size > 1) {
                    item(key = "episodeRanges") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            items(episodeChunks.indices.toList()) { chunkIndex ->
                                val start = chunkIndex * episodeChunkSize + 1
                                val end = minOf((chunkIndex + 1) * episodeChunkSize, current.episodes.size)
                                FilterChip(
                                    selected = chunkIndex == selectedEpisodeChunk,
                                    onClick = { selectedEpisodeChunk = chunkIndex },
                                    label = { Text("$start–$end") },
                                )
                            }
                        }
                    }
                }
                val visibleEpisodes = episodeChunks.getOrElse(selectedEpisodeChunk) { current.episodes }
                items(visibleEpisodes.chunked(4), key = { row -> "row-${row.first().id}" }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { episode ->
                            EpisodeChip(
                                number = episode.number,
                                onClick = { onEpisodeClick(current.show, episode) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            }
        }

        val loadedState = state as? AnimeDetailState.Loaded
        if (showPlanner && loadedState != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showPlanner = false }, sheetState = sheetState) {
                DownloadPlannerSheet(
                    title = "Download episodes",
                    units = loadedState.episodes.map { PlannerUnit(it.id, "Episode ${it.number}") },
                    onConfirm = { selectedIds, quality ->
                        showPlanner = false
                        val selectedEpisodes = loadedState.episodes.filter { it.id in selectedIds }
                        scope.launch {
                            selectedEpisodes.forEach { episode ->
                                val sources = runCatching { animeApi.stream(loadedState.show.id, episode.number) }
                                    .getOrDefault(emptyList())
                                    .playbackSources()
                                val chosen = sources.firstOrNull { it.label == quality } ?: sources.firstOrNull()
                                if (chosen != null) {
                                    videoDownloadManager.startDownload(
                                        contentType = DownloadContentType.ANIME,
                                        contentId = loadedState.show.id,
                                        contentTitle = loadedState.show.title,
                                        unitId = episode.number.toString(),
                                        unitTitle = "Episode ${episode.number}",
                                        imageUrl = loadedState.show.imageUrl,
                                        source = chosen,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * A single episode number as a compact tappable tile. Long-running shows (500+ episodes) made a
 * one-row-per-episode list absurdly long to scroll; a 4-wide grid plus the range chips above it
 * mirrors AsterionMac's long-series episode browser.
 */
@Composable
private fun EpisodeChip(number: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", style = MaterialTheme.typography.labelLarge)
    }
}
