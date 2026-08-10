package cloud.cyberverse.asterion.ui.novels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.download.NovelDownloadRepository
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionFilledButton
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.ExpandableSynopsis
import cloud.cyberverse.asterion.ui.components.SectionHeader
import cloud.cyberverse.asterion.ui.downloads.DownloadPlannerSheet
import cloud.cyberverse.asterion.ui.downloads.PlannerUnit
import cloud.cyberverse.asterion.ui.theme.genreColor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val CHAPTER_PAGE_SIZE = 40
private const val LOAD_MORE_THRESHOLD = 8

// Fixed items ahead of the chapter list in the LazyColumn (hero, synopsis, chapters header) -
// used to translate a chapter's index into its absolute LazyColumn item index for scroll-to-jumps.
private const val CHAPTER_LIST_OFFSET = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelId: String,
    onChapterClick: (Chapter) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: NovelDetailViewModel = koinViewModel(parameters = { parametersOf(novelId) }),
    novelDownloadRepository: NovelDownloadRepository = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    var showPlanner by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val showTitleInBar by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 500 }
    }

    Scaffold(
        topBar = {
            val loaded = state as? NovelDetailState.Loaded
            AsterionTopBar(
                title = if (showTitleInBar) loaded?.novel?.title else null,
                onBack = onNavigateBack,
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = viewModel::toggleBookmark, enabled = !loaded.isBookmarkUpdating) {
                            Icon(
                                if (loaded.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (loaded.isBookmarked) "Remove from library" else "Save to library",
                            )
                        }
                        IconButton(onClick = { showPlanner = true }) {
                            Icon(Icons.Filled.Download, contentDescription = "Download chapters")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is NovelDetailState.Loading -> AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))

            is NovelDetailState.Error -> ErrorState(
                message = current.message,
                // A person tapping retry wants a fresh attempt, not whatever we cached.
                onRetry = { viewModel.load(forceRefresh = true) },
                modifier = Modifier.padding(padding),
            )

            is NovelDetailState.Loaded -> {
                var visibleChapterCount by rememberSaveable(current.chapters.size) {
                    mutableStateOf(minOf(CHAPTER_PAGE_SIZE, current.chapters.size))
                }

                LaunchedEffect(listState, current.chapters.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .collect { lastVisibleIndex ->
                            val lastVisibleChapterIndex = lastVisibleIndex - CHAPTER_LIST_OFFSET
                            if (lastVisibleChapterIndex >= visibleChapterCount - LOAD_MORE_THRESHOLD &&
                                visibleChapterCount < current.chapters.size
                            ) {
                                visibleChapterCount = minOf(visibleChapterCount + CHAPTER_PAGE_SIZE, current.chapters.size)
                            }
                        }
                }

                fun jumpToChapterIndex(index: Int) {
                    if (index < 0 || index >= current.chapters.size) return
                    if (index >= visibleChapterCount) {
                        visibleChapterCount = minOf(index + 1, current.chapters.size)
                    }
                    // scrollToItem, not animateScrollToItem: animating to chapter 4,000 scrolls
                    // through every item in between, which is a long ride, not a jump.
                    scope.launch { listState.scrollToItem(CHAPTER_LIST_OFFSET + index) }
                }

                LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
                    item(key = "hero") {
                        val resumeChapter = current.resumeChapterNumber
                            ?.let { number -> current.chapters.firstOrNull { it.chapterNumber == number } }
                        NovelHero(
                            novel = current.novel,
                            totalChapters = current.totalChapters,
                            resumeChapter = resumeChapter,
                            onStartReading = { (resumeChapter ?: current.chapters.firstOrNull())?.let(onChapterClick) },
                        )
                    }

                    item(key = "synopsis") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            SectionHeader(title = "Synopsis")
                            ExpandableSynopsis(current.novel.summary, modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    item(key = "chaptersHeader") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            SectionHeader(
                                title = "Chapters",
                                subtitle = when {
                                    current.isLoadingChapters ->
                                        "${current.chapters.size} of ${current.totalChapters} loaded…"
                                    current.isChapterListPartial ->
                                        "${current.chapters.size} of ${current.totalChapters} chapters available"
                                    else -> "${current.totalChapters} chapters"
                                },
                            )
                            if (current.chapters.size > CHAPTER_PAGE_SIZE) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 10.dp),
                                ) {
                                    OutlinedButton(onClick = { jumpToChapterIndex(0) }) { Text("First chapter") }
                                    OutlinedButton(onClick = { jumpToChapterIndex(current.chapters.lastIndex) }) { Text("Latest chapter") }
                                }
                            }
                        }
                    }

                    items(current.chapters.take(visibleChapterCount), key = { it.id }) { chapter ->
                        ListItem(
                            headlineContent = { Text(chapter.title) },
                            leadingContent = {
                                Text(
                                    "${chapter.chapterNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                            modifier = Modifier.clickable { onChapterClick(chapter) },
                        )
                    }

                    if (visibleChapterCount < current.chapters.size) {
                        item(key = "chapterLoadingFooter") {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }

        val loadedState = state as? NovelDetailState.Loaded
        if (showPlanner && loadedState != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showPlanner = false }, sheetState = sheetState) {
                DownloadPlannerSheet(
                    title = "Download chapters",
                    units = loadedState.chapters.map { PlannerUnit(it.id, "${it.chapterNumber}. ${it.title}") },
                    onConfirm = { selectedIds, _ ->
                        showPlanner = false
                        loadedState.chapters.filter { it.id in selectedIds }.forEach { chapter ->
                            novelDownloadRepository.downloadChapter(loadedState.novel, chapter.chapterNumber, chapter.title)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NovelHero(
    novel: Novel,
    totalChapters: Int,
    resumeChapter: Chapter?,
    onStartReading: () -> Unit,
) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsterionAsyncImage(
            model = novel.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .width(156.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            novel.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        novel.author?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            novel.genres?.firstOrNull()?.let { genre ->
                MetadataChip(icon = null, text = genre, tint = genreColor(novel.genres))
            }
            if (totalChapters > 0) MetadataChip(Icons.Filled.AutoStories, "$totalChapters ch.")
            novel.rating?.let { MetadataChip(Icons.Filled.Star, it.toString()) }
            novel.views?.let { MetadataChip(Icons.Filled.Visibility, it) }
        }

        AsterionFilledButton(
            text = if (resumeChapter != null) "Continue Chapter ${resumeChapter.chapterNumber}" else "Start Reading",
            icon = Icons.Filled.AutoStories,
            onClick = onStartReading,
            modifier = Modifier.padding(top = 22.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun MetadataChip(icon: androidx.compose.ui.graphics.vector.ImageVector?, text: String, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.width(16.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
