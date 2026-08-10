package cloud.cyberverse.asterion.ui.novels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.cache.NovelContentCache
import cloud.cyberverse.asterion.ui.common.userMessage
import cloud.cyberverse.asterion.data.model.SaveProgressRequest
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.data.remote.fetchAllChapters
import java.io.File
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

sealed interface ChapterReaderState {
    data object Loading : ChapterReaderState
    data class Loaded(val chapter: Chapter, val allChapters: List<Chapter>, val isOffline: Boolean = false) : ChapterReaderState
    data class Error(val message: String) : ChapterReaderState
}

class ChapterReaderViewModel(
    private val api: AsterionApiService,
    private val cache: NovelContentCache,
    private val novelId: String,
    private val chapterNumber: Int,
    private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<ChapterReaderState>(ChapterReaderState.Loading)
    val state: StateFlow<ChapterReaderState> = _state.asStateFlow()

    // Scroll position changes fire on every pixel; buffering just the latest and debouncing the
    // collector is what keeps this from hammering /me/progress on every frame of a scroll.
    private val progressUpdates = MutableSharedFlow<Pair<Int, Int>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        viewModelScope.launch {
            progressUpdates.debounce(1_500).distinctUntilChanged().collect { (currentLine, totalLines) ->
                val chapterId = (_state.value as? ChapterReaderState.Loaded)?.chapter?.id ?: return@collect
                val percentage = if (totalLines > 0) currentLine.toDouble() / totalLines * 100 else 0.0
                runCatching {
                    api.saveProgress(SaveProgressRequest(novelId, chapterId, currentLine, totalLines, percentage))
                }
            }
        }

        load()
    }

    fun load() {
        _state.value = ChapterReaderState.Loading
        viewModelScope.launch {
            // A chapter downloaded for offline reading lives at a deterministic path - checking
            // it directly means offline reading works without needing a separate index lookup.
            val downloadedChapter = readDownloadedChapter(novelId, chapterNumber)
            if (downloadedChapter != null) {
                val allChapters = chapterList()
                _state.value = ChapterReaderState.Loaded(downloadedChapter, allChapters, isOffline = true)
                return@launch
            }

            _state.value = try {
                val chapter = api.chapter(novelId, chapterNumber).data
                // The full list only powers the picker/prev-next nav - if it fails to load,
                // the chapter itself already succeeded, so don't fail the whole screen over it.
                val allChapters = chapterList()
                ChapterReaderState.Loaded(chapter, allChapters)
            } catch (error: Exception) {
                val fallback = readDownloadedChapter(novelId, chapterNumber)
                if (fallback != null) {
                    ChapterReaderState.Loaded(fallback, emptyList(), isOffline = true)
                } else {
                    ChapterReaderState.Error(error.userMessage())
                }
            }
        }
    }

    /**
     * The picker and prev/next nav need the whole chapter list, but this screen is recreated on
     * every chapter navigation, so fetching it here re-ran the full page crawl per chapter. Shared
     * with the detail screen through the cache; failure is non-fatal because the chapter itself
     * has already loaded.
     */
    private suspend fun chapterList(): List<Chapter> {
        cache.chapters(novelId)?.let { return it }
        val crawl = runCatching { api.fetchAllChapters(novelId) }.getOrNull() ?: return emptyList()
        // Only cache a list we know is whole - a truncated crawl would otherwise become the
        // chapter list the detail screen serves from too.
        if (crawl.isComplete) cache.putChapters(novelId, crawl.chapters)
        return crawl.chapters
    }

    /** Reports the reader's current scroll position within this chapter, synced to the account so
     * "Continue Chapter N" on the novel's detail page reflects where the reader actually is. */
    fun reportProgress(currentLine: Int, totalLines: Int) {
        progressUpdates.tryEmit(currentLine to totalLines)
    }

    private fun readDownloadedChapter(novelId: String, chapterNumber: Int): Chapter? {
        val file = File(context.filesDir, "downloads/novels/$novelId/$chapterNumber.json")
        if (!file.exists()) return null
        return runCatching { json.decodeFromString<Chapter>(file.readText()) }.getOrNull()
    }
}
