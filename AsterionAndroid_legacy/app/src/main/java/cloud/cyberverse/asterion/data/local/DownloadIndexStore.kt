package cloud.cyberverse.asterion.data.local

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

/**
 * The persisted index of every download the user has started, across movies/anime/novels.
 * A single JSON file rather than a database - mirrors AsterionMac's own MediaDownloadManager
 * index (a schema-versioned JSON file), and this app's download count is small (dozens, not
 * thousands), so a database's query power isn't needed, just durability across process death.
 */
class DownloadIndexStore(context: Context) {
    private val indexFile = File(context.filesDir, "downloads/index.json")
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                _entries.value = runCatching {
                    if (indexFile.exists()) json.decodeFromString<List<DownloadEntry>>(indexFile.readText()) else emptyList()
                }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun upsert(entry: DownloadEntry) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = _entries.value.filterNot { it.id == entry.id } + entry.copy(updatedAt = System.currentTimeMillis())
                _entries.value = updated.sortedByDescending { it.updatedAt }
                persist(updated)
            }
        }
    }

    /** Atomic read-modify-write, so concurrent updaters (download progress vs. subtitle fetch) can't clobber each other. */
    suspend fun update(id: String, transform: (DownloadEntry?) -> DownloadEntry?) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = _entries.value.firstOrNull { it.id == id }
                val next = transform(current) ?: return@withLock
                val updated = _entries.value.filterNot { it.id == id } + next.copy(updatedAt = System.currentTimeMillis())
                _entries.value = updated.sortedByDescending { it.updatedAt }
                persist(updated)
            }
        }
    }

    suspend fun remove(id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = _entries.value.filterNot { it.id == id }
                _entries.value = updated
                persist(updated)
            }
        }
    }

    fun get(id: String): DownloadEntry? = _entries.value.firstOrNull { it.id == id }

    private fun persist(entries: List<DownloadEntry>) {
        indexFile.parentFile?.mkdirs()
        // Write-then-rename so a crash mid-write never leaves a half-written, unparseable index.
        val tempFile = File(indexFile.parentFile, "${indexFile.name}.tmp")
        tempFile.writeText(json.encodeToString(entries))
        tempFile.renameTo(indexFile)
    }
}
