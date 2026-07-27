package cloud.cyberverse.asterion.ui.novels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.AddToLibraryRequest
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.data.model.Novel
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : NovelDetailState
    data class Error(val message: String) : NovelDetailState
}

class NovelDetailViewModel(private val api: AsterionApiService, private val novelId: String) : ViewModel() {
    private val _state = MutableStateFlow<NovelDetailState>(NovelDetailState.Loading)
    val state: StateFlow<NovelDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val novel = api.novel(novelId).data
                val chapters = api.chapters(novelId).data
                // Bookmark/progress sync is best-effort: a signed-in reader without network (or
                // hitting an auth hiccup) should still see the novel, just without sync state -
                // both run in parallel with the chapter list already loaded.
                coroutineScope {
                    val bookmarkedDeferred = async {
                        runCatching { api.library().data.any { it.novelId == novelId } }.getOrDefault(false)
                    }
                    val progressDeferred = async {
                        runCatching { api.progressForNovel(novelId).data }.getOrNull()
                    }
                    val resumeChapterNumber = progressDeferred.await()
                        ?.let { progress -> chapters.firstOrNull { it.id == progress.chapterId }?.chapterNumber }
                    NovelDetailState.Loaded(
                        novel = novel,
                        chapters = chapters,
                        isBookmarked = bookmarkedDeferred.await(),
                        resumeChapterNumber = resumeChapterNumber,
                    )
                }
            } catch (error: Exception) {
                NovelDetailState.Error(error.message ?: "Unknown error")
            }
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
