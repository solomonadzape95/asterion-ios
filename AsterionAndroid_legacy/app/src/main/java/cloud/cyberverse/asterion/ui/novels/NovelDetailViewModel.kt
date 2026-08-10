package cloud.cyberverse.asterion.ui.novels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.AddToLibraryRequest
import cloud.cyberverse.asterion.data.cache.NovelContentCache
import cloud.cyberverse.asterion.ui.common.userMessage
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.data.remote.ChapterPage
import cloud.cyberverse.asterion.data.remote.fetchAllChapters
import cloud.cyberverse.asterion.data.remote.fetchChapterPage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NovelDetailState {
    data object Loading : NovelDetailState
    data class Loaded(
        val novel: Novel,
        val chapters: List<Chapter>,
        val isBookmarked: Boolean = false,
        val isBookmarkUpdating: Boolean = false,
        // Which chapter to resume into, from synced reading progress - null means no synced
        // progress exists (or it couldn't be fetched), so "Start Reading" opens chapter one.
        val resumeChapterNumber: Int? = null,
        /**
         * The single number to show a reader. Counted by the server from the chapters that
         * actually exist, rather than the novel record's scraped `totalChapters` string, which is
         * frequently stale. While the rest of the list pages in, this is larger than
         * [chapters].size - that is the point: the reader sees the real length immediately.
         */
        val totalChapters: Int = chapters.size,
        /** True while later pages are still arriving. */
        val isLoadingChapters: Boolean = false,
        /** The crawl stopped early, so [chapters] is not the whole novel. */
        val isChapterListPartial: Boolean = false,
    ) : NovelDetailState
    data class Error(val message: String) : NovelDetailState
}

class NovelDetailViewModel(
    private val api: AsterionApiService,
    private val cache: NovelContentCache,
    private val novelId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<NovelDetailState>(NovelDetailState.Loading)
    val state: StateFlow<NovelDetailState> = _state.asStateFlow()

    /** Kept so resume can be re-resolved once later chapter pages arrive. */
    private var resumeChapterId: String? = null

    init {
        load()
    }

    /** [forceRefresh] skips the cache, for an explicit pull-to-refresh or retry. */
    fun load(forceRefresh: Boolean = false) {
        _state.value = NovelDetailState.Loading
        viewModelScope.launch {
            if (forceRefresh) cache.invalidate(novelId)
            try {
                val novel = cache.novel(novelId)
                    ?: api.novel(novelId).data.also { cache.putNovel(novelId, it) }

                val cachedChapters = cache.chapters(novelId)
                // Render the first page rather than blocking on the whole crawl. A 4,000-chapter
                // novel is 40 sequential round trips; waiting for all of them before showing
                // anything is what made opening a long novel feel broken.
                val firstPage = cachedChapters?.let { ChapterPage(it, it.size) }
                    ?: api.fetchChapterPage(novelId)

                _state.value = NovelDetailState.Loaded(
                    novel = novel,
                    chapters = firstPage.chapters,
                    totalChapters = firstPage.total,
                    isLoadingChapters = firstPage.chapters.size < firstPage.total,
                )

                // Both refine what is already on screen, and neither should wait on the other -
                // the bookmark button must not sit inert behind a 40-request chapter crawl.
                coroutineScope {
                    launch { if (cachedChapters == null) loadRemainingChapters(firstPage) }
                    launch { loadAccountState() }
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.value = NovelDetailState.Error(error.userMessage())
            }
        }
    }

    private suspend fun loadRemainingChapters(firstPage: ChapterPage) {
        if (firstPage.chapters.size >= firstPage.total) {
            cache.putChapters(novelId, firstPage.chapters)
            return
        }

        val crawl = try {
            api.fetchAllChapters(novelId, startingFrom = firstPage.chapters)
        } catch (error: Exception) {
            if (error is kotlin.coroutines.cancellation.CancellationException) throw error
            // The first page is already on screen and usable; a failure here must not replace a
            // working novel with an error.
            _state.update { current ->
                (current as? NovelDetailState.Loaded)
                    ?.copy(isLoadingChapters = false, isChapterListPartial = true)
                    ?: current
            }
            return
        }

        if (crawl.isComplete) cache.putChapters(novelId, crawl.chapters)
        _state.update { current ->
            (current as? NovelDetailState.Loaded)?.copy(
                chapters = crawl.chapters,
                totalChapters = maxOf(crawl.total, crawl.chapters.size),
                isLoadingChapters = false,
                isChapterListPartial = !crawl.isComplete,
                // Progress may have resolved against page one before the chapter it points at
                // had arrived, so re-resolve now that the full list is here.
                resumeChapterNumber = current.resumeChapterNumber
                    ?: resumeChapterId?.let { id -> crawl.chapters.firstOrNull { it.id == id }?.chapterNumber },
            ) ?: current
        }
    }

    /**
     * Bookmark and progress sync are best-effort: a reader without network, or hitting an auth
     * hiccup, should still get the novel - just without sync state.
     */
    private suspend fun loadAccountState() = coroutineScope {
        val bookmarkedDeferred = async {
            runCatching { api.library().data.any { it.novelId == novelId } }.getOrDefault(false)
        }
        val progressDeferred = async {
            runCatching { api.progressForNovel(novelId).data }.getOrNull()
        }
        val progress = progressDeferred.await()
        val isBookmarked = bookmarkedDeferred.await()
        resumeChapterId = progress?.chapterId

        _state.update { current ->
            (current as? NovelDetailState.Loaded)?.copy(
                isBookmarked = isBookmarked,
                // Resolved against whatever chapters have arrived so far; if progress points into
                // a page still loading, the crawl re-resolves it when the rest lands.
                resumeChapterNumber = progress
                    ?.let { p -> current.chapters.firstOrNull { it.id == p.chapterId }?.chapterNumber },
            ) ?: current
        }
    }

    fun toggleBookmark() {
        val current = _state.value as? NovelDetailState.Loaded ?: return
        if (current.isBookmarkUpdating) return
        _state.value = current.copy(isBookmarkUpdating = true)
        viewModelScope.launch {
            val nowBookmarked = try {
                if (current.isBookmarked) {
                    api.removeFromLibrary(novelId)
                    false
                } else {
                    api.addToLibrary(AddToLibraryRequest(novelId))
                    true
                }
            } catch (error: Exception) {
                current.isBookmarked
            }
            val latest = _state.value as? NovelDetailState.Loaded ?: return@launch
            _state.value = latest.copy(isBookmarked = nowBookmarked, isBookmarkUpdating = false)
        }
    }
}
