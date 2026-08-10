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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.download.NovelDownloadRepository
import cloud.cyberverse.asterion.ui.components.PhosphorIcons
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.ui.components.DetailSkeleton
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionFilledButton
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.ExpandableSynopsis
import cloud.cyberverse.asterion.ui.components.SectionHeader
import cloud.cyberverse.asterion.ui.downloads.DownloadPlannerSheet
import cloud.cyberverse.asterion.ui.downloads.PlannerUnit
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import cloud.cyberverse.asterion.ui.components.AsterionIconButton
import cloud.cyberverse.asterion.ui.components.BlurredArtworkBanner
import cloud.cyberverse.asterion.ui.theme.CoverCornerRadius
import cloud.cyberverse.asterion.ui.theme.PillShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.AsterionOutlinedButton
import cloud.cyberverse.asterion.ui.components.BackToTopButton
import cloud.cyberverse.asterion.ui.components.FastScrollbar
import cloud.cyberverse.asterion.ui.settings.AppSettings
import cloud.cyberverse.asterion.ui.settings.AppSettingsPreferences
import cloud.cyberverse.asterion.ui.theme.genreColor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val appSettings: AppSettingsPreferences = koinInject()
    val settings by appSettings.settings.collectAsState(initial = AppSettings())
    val chapterLayout = settings.chapterLayout

    val showTitleInBar by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 500 }
    }

    Scaffold(
        topBar = {
            val loaded = state as? NovelDetailState.Loaded
            AsterionTopBar(
                title = if (showTitleInBar) loaded?.novel?.title else null,
                onBack = onNavigateBack,
                actions = {
                    // These live in the hero now. Duplicating them in the bar while the hero is
                    // visible puts the same two controls on screen twice; they reappear here only
                    // once the hero has scrolled away and taken them with it.
                    if (loaded != null && showTitleInBar) {
                        IconButton(onClick = viewModel::toggleBookmark, enabled = !loaded.isBookmarkUpdating) {
                            Icon(
                                if (loaded.isBookmarked) PhosphorIcons.Bookmark else PhosphorIcons.BookmarkBorder,
                                contentDescription = if (loaded.isBookmarked) "Remove from library" else "Save to library",
                            )
                        }
                        IconButton(onClick = { showPlanner = true }) {
                            Icon(PhosphorIcons.Download, contentDescription = "Download chapters")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is NovelDetailState.Loading -> DetailSkeleton(Modifier.padding(padding))

            is NovelDetailState.Error -> ErrorState(
                message = current.message,
                // A person tapping retry wants a fresh attempt, not whatever we cached.
                onRetry = { viewModel.load(forceRefresh = true) },
                modifier = Modifier.padding(padding),
            )

            is NovelDetailState.Loaded -> {
                val chapters = current.chapters
                // hero + synopsis + header, plus the range rail only when it is shown.
                val headerCount = if (chapters.size > CHAPTER_RANGE_SIZE) 4 else 3
                var selectedRange by rememberSaveable(chapters.size) { mutableStateOf(0) }
                var showJumpDialog by remember { mutableStateOf(false) }

                // One grid drives both layouts: list mode is simply a single-column grid. That
                // keeps one scroll state, so the scrollbar, range rail and back-to-top all work
                // identically in either mode instead of needing a parallel implementation.
                val columns = if (chapterLayout == ChapterLayout.GRID) 3 else 1

                fun scrollToChapterIndex(index: Int) {
                    if (index < 0 || index >= chapters.size) return
                    // scrollToItem, not animateScrollToItem: animating to chapter 4,000 would
                    // travel through every item in between, which is a ride, not a jump.
                    scope.launch { gridState.scrollToItem(headerCount + index) }
                }

                fun jumpToChapterNumber(number: Int) {
                    val index = chapters.indexOfFirst { it.chapterNumber == number }
                    if (index >= 0) {
                        selectedRange = index / CHAPTER_RANGE_SIZE
                        scrollToChapterIndex(index)
                    }
                }

                // Keep the range rail in step with where the reader actually is, so scrolling
                // updates the chips rather than leaving them stale.
                LaunchedEffect(gridState, chapters.size) {
                    snapshotFlow { gridState.firstVisibleItemIndex }
                        .collect { first ->
                            val chapterIndex = (first - headerCount).coerceAtLeast(0)
                            selectedRange = chapterIndex / CHAPTER_RANGE_SIZE
                        }
                }

                Box(Modifier.padding(padding)) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                            val resumeChapter = current.resumeChapterNumber
                                ?.let { number -> chapters.firstOrNull { it.chapterNumber == number } }
                            NovelHero(
                                novel = current.novel,
                                totalChapters = current.totalChapters,
                                resumeChapter = resumeChapter,
                                isBookmarked = current.isBookmarked,
                                isBookmarkUpdating = current.isBookmarkUpdating,
                                onStartReading = { (resumeChapter ?: chapters.firstOrNull())?.let(onChapterClick) },
                                onToggleBookmark = viewModel::toggleBookmark,
                                onDownload = { showPlanner = true },
                            )
                        }

                        item(key = "synopsis", span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                SectionHeader(title = "Synopsis")
                                ExpandableSynopsis(current.novel.summary, modifier = Modifier.padding(top = 8.dp))
                            }
                        }

                        item(key = "chaptersHeader", span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        SectionHeader(
                                            title = "Chapters",
                                            subtitle = when {
                                                current.isLoadingChapters ->
                                                    "${chapters.size} of ${current.totalChapters} loaded…"
                                                current.isChapterListPartial ->
                                                    "${chapters.size} of ${current.totalChapters} available"
                                                else -> "${current.totalChapters} chapters"
                                            },
                                        )
                                    }
                                    ChapterLayoutToggle(
                                        layout = chapterLayout,
                                        onLayoutChange = { next ->
                                            scope.launch { appSettings.setChapterLayout(next) }
                                        },
                                    )
                                }

                                if (chapters.size > 1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 12.dp),
                                    ) {
                                        AsterionOutlinedButton(
                                            text = "First",
                                            icon = PhosphorIcons.ChevronLeft,
                                            // These open a chapter now. Previously they only
                                            // scrolled the list to it, which is not what
                                            // "read the first chapter" means.
                                            onClick = { chapters.firstOrNull()?.let(onChapterClick) },
                                            modifier = Modifier.weight(1f),
                                        )
                                        AsterionOutlinedButton(
                                            text = "Latest",
                                            icon = PhosphorIcons.ChevronRight,
                                            onClick = { chapters.lastOrNull()?.let(onChapterClick) },
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (chapters.size > CHAPTER_RANGE_SIZE) {
                                            AsterionIconButton(
                                                icon = PhosphorIcons.JumpTo,
                                                contentDescription = "Jump to chapter",
                                                onClick = { showJumpDialog = true },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (chapters.size > CHAPTER_RANGE_SIZE) {
                            item(key = "rangeRail", span = { GridItemSpan(maxLineSpan) }) {
                                ChapterRangeRail(
                                    totalChapters = chapters.size,
                                    selectedRange = selectedRange,
                                    onRangeSelected = { range ->
                                        selectedRange = range
                                        scrollToChapterIndex(range * CHAPTER_RANGE_SIZE)
                                    },
                                )
                            }
                        }

                        // No manual windowing on top of the grid: LazyVerticalGrid already only
                        // composes what is visible, and the old visibleChapterCount slice
                        // reallocated a sublist on every recomposition.
                        items(chapters, key = { it.id }) { chapter ->
                            val isRead = current.resumeChapterNumber?.let { chapter.chapterNumber < it } == true
                            if (chapterLayout == ChapterLayout.GRID) {
                                ChapterTile(
                                    chapter = chapter,
                                    isRead = isRead,
                                    isDownloaded = false,
                                    onClick = { onChapterClick(chapter) },
                                    modifier = Modifier.animateItem(),
                                )
                            } else {
                                ChapterRow(
                                    chapter = chapter,
                                    isRead = isRead,
                                    isDownloaded = false,
                                    onClick = { onChapterClick(chapter) },
                                    modifier = Modifier.animateItem().padding(horizontal = 20.dp),
                                )
                            }
                        }

                        if (current.isLoadingChapters) {
                            item(key = "chapterLoadingFooter", span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    AsterionLoadingIndicator()
                                }
                            }
                        }
                    }

                    FastScrollbar(
                        state = gridState,
                        totalItems = headerCount + chapters.size,
                        label = { index ->
                            chapters.getOrNull(index - headerCount)?.let { "Ch. ${it.chapterNumber}" } ?: ""
                        },
                    )

                    BackToTopButton(
                        visible = gridState.firstVisibleItemIndex > headerCount,
                        onClick = { scope.launch { gridState.scrollToItem(0) } },
                    )
                }

                if (showJumpDialog) {
                    JumpToChapterDialog(
                        totalChapters = current.totalChapters,
                        onDismiss = { showJumpDialog = false },
                        onJump = { number ->
                            showJumpDialog = false
                            jumpToChapterNumber(number)
                        },
                    )
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
    isBookmarked: Boolean,
    isBookmarkUpdating: Boolean,
    onStartReading: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDownload: () -> Unit,
) {
    Column {
        BlurredArtworkBanner(model = novel.imageUrl, height = 300.dp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // The sharp cover against its own blurred enlargement is what gives the header
                // depth; a shadow separates it from the wash behind it.
                AsterionAsyncImage(
                    model = novel.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(116.dp)
                        .aspectRatio(2f / 3f)
                        .shadow(18.dp, RoundedCornerShape(CoverCornerRadius), clip = false)
                        .clip(RoundedCornerShape(CoverCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Column(Modifier.weight(1f).padding(bottom = 6.dp)) {
                    Text(
                        novel.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    novel.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // Metadata scrolls sideways rather than wrapping or truncating - a novel with a long genre
        // plus a six-figure view count will not fit on one line of a phone.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            novel.genres?.firstOrNull()?.let { genre ->
                MetadataChip(text = genre, tint = genreColor(novel.genres), emphasised = true)
            }
            if (totalChapters > 0) MetadataChip(PhosphorIcons.AutoStories, "$totalChapters chapters")
            novel.rating?.let { MetadataChip(PhosphorIcons.Star, it.toString()) }
            novel.views?.let { MetadataChip(PhosphorIcons.Visibility, it) }
            novel.status?.let { MetadataChip(text = it) }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 4.dp),
        ) {
            AsterionFilledButton(
                text = if (resumeChapter != null) "Continue Ch. ${resumeChapter.chapterNumber}" else "Start Reading",
                icon = PhosphorIcons.AutoStories,
                onClick = onStartReading,
                modifier = Modifier.weight(1f),
            )
            AsterionIconButton(
                icon = if (isBookmarked) PhosphorIcons.Bookmark else PhosphorIcons.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove from library" else "Save to library",
                onClick = onToggleBookmark,
                enabled = !isBookmarkUpdating,
                selected = isBookmarked,
            )
            AsterionIconButton(
                icon = PhosphorIcons.Download,
                contentDescription = "Download chapters",
                onClick = onDownload,
            )
        }
    }
}

/**
 * A pill-shaped metadata tag. This used to be a bare Row of icon-plus-text with no background,
 * which is why the hero read as loose text rather than as structured metadata.
 */
@Composable
private fun MetadataChip(
    icon: ImageVector? = null,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    emphasised: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(PillShape)
            .background(
                if (emphasised) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}
