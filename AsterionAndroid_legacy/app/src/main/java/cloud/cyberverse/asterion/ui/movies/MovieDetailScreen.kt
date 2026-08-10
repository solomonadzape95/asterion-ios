package cloud.cyberverse.asterion.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.ui.components.PhosphorIcons
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.model.MovieShow
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionFilledButton
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.ExpandableSynopsis
import cloud.cyberverse.asterion.ui.components.PlaybackSourceKind
import cloud.cyberverse.asterion.ui.components.SectionHeader
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    slug: String,
    onPlayClick: (MovieShow) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: MovieDetailViewModel = koinViewModel(parameters = { parametersOf(slug) }),
    videoDownloadManager: VideoDownloadManager = koinInject(),
    movieApi: MovieApiService = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AsterionTopBar(
                onBack = onNavigateBack,
                actions = {
                    val loaded = state as? MovieDetailState.Loaded
                    if (loaded != null) {
                        IconButton(onClick = viewModel::toggleBookmark, enabled = !loaded.isBookmarkUpdating) {
                            Icon(
                                if (loaded.isBookmarked) PhosphorIcons.Bookmark else PhosphorIcons.BookmarkBorder,
                                contentDescription = if (loaded.isBookmarked) "Remove from saved" else "Save",
                            )
                        }
                        IconButton(
                            onClick = {
                                isDownloading = true
                                scope.launch {
                                    // A WEB-kind source is an embed page with no direct URL - it
                                    // can't be saved as a file, so downloads only ever consider
                                    // DIRECT sources.
                                    val source = runCatching { movieApi.playback(slug).sources.toPlaybackSources() }
                                        .getOrDefault(emptyList())
                                        .firstOrNull { it.kind == PlaybackSourceKind.DIRECT }
                                    if (source == null) {
                                        isDownloading = false
                                        return@launch
                                    }
                                    videoDownloadManager.startDownload(
                                        contentType = DownloadContentType.MOVIE,
                                        contentId = slug,
                                        contentTitle = loaded.show.title,
                                        unitId = slug,
                                        unitTitle = null,
                                        imageUrl = loaded.show.imageUrl,
                                        source = source,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                if (isDownloading) PhosphorIcons.DownloadDone else PhosphorIcons.Download,
                                contentDescription = "Download",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is MovieDetailState.Loading -> AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))

            is MovieDetailState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            is MovieDetailState.Loaded -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsterionAsyncImage(
                        model = current.show.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 20.dp)
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
                    current.show.director?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }

                    val ratings = remember(current.show) { ratingsFor(current.show) }
                    if (ratings.isNotEmpty()) {
                        RatingsStrip(ratings, modifier = Modifier.padding(top = 12.dp))
                    }

                    AsterionFilledButton(
                        text = if (current.show.isSeries) "Play S1 · E1" else "Play",
                        icon = PhosphorIcons.PlayCircle,
                        onClick = { onPlayClick(current.show) },
                        modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
                    )
                }

                val facts = remember(current.show) { factsFor(current.show) }
                if (facts.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp)) {
                        SectionHeader(title = "Details")
                        FactsGrid(facts, modifier = Modifier.padding(top = 10.dp))
                    }
                }

                if (current.show.actors.isNotEmpty()) {
                    Column(Modifier.padding(top = 26.dp)) {
                        SectionHeader(
                            title = "Cast",
                            subtitle = if (current.show.actors.size == 1) "1 person" else "${current.show.actors.size} people",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        CastShelf(current.show.actors, modifier = Modifier.padding(top = 10.dp))
                    }
                }

                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp)) {
                    SectionHeader(title = "Synopsis")
                    ExpandableSynopsis(current.show.description, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

private data class MovieRating(val source: String, val value: String)
private data class MovieFact(val label: String, val value: String)

private fun blankToNull(value: String?): String? = value?.trim()?.ifEmpty { null }

private fun ratingsFor(show: MovieShow): List<MovieRating> = listOfNotNull(
    blankToNull(show.imdbRating)?.let { MovieRating("IMDb", it) },
    blankToNull(show.tmdbRating)?.let { MovieRating("TMDB", it) },
    blankToNull(show.rottenTomatoes)?.let { MovieRating("Rotten Tomatoes", it) },
    blankToNull(show.metacritic)?.let { MovieRating("Metacritic", it) },
)

private fun factsFor(show: MovieShow): List<MovieFact> = listOfNotNull(
    MovieFact("Format", if (show.isSeries) "TV Series" else "Movie"),
    blankToNull(show.releaseDate)?.let { MovieFact("Release date", it) },
    blankToNull(show.country)?.let { MovieFact("Country", it) },
    blankToNull(show.duration)?.let { MovieFact("Runtime", it) },
    blankToNull(show.director)?.let { MovieFact("Director", it) },
    show.genres.takeIf { it.isNotEmpty() }?.let { MovieFact("Genres", it.joinToString(" · ")) },
    show.seasons.takeIf { it.isNotEmpty() }?.let { MovieFact("Seasons", it.joinToString(" · ")) },
)

@Composable
private fun RatingsStrip(ratings: List<MovieRating>, modifier: Modifier = Modifier) {
    // height(IntrinsicSize.Min) + fillMaxHeight on the divider is required for a Row divider to
    // actually render - a divider with no height modifier and no cross-axis stretch collapses to
    // zero height and simply doesn't draw, which is why this looked broken before.
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ratings.forEachIndexed { index, rating ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(PhosphorIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text(
                    rating.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    rating.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun FactsGrid(facts: List<MovieFact>, modifier: Modifier = Modifier) {
    Column(modifier) {
        facts.forEachIndexed { index, fact ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                )
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(
                    fact.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(110.dp),
                )
                Text(
                    fact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CastShelf(actors: List<String>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        items(actors) { actor ->
            Text(
                actor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
