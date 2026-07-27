package cloud.cyberverse.asterion.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.data.local.DownloadEntry
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DownloadsViewModel(
    downloadIndexStore: DownloadIndexStore,
    private val videoDownloadManager: VideoDownloadManager,
) : ViewModel() {
    val entries: StateFlow<List<DownloadEntry>> = downloadIndexStore.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(entry: DownloadEntry) {
        videoDownloadManager.cancelOrRemove(entry)
    }
}
