package cloud.cyberverse.asterion.data.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.local.DownloadEntry
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.data.local.DownloadPhase
import cloud.cyberverse.asterion.data.local.DownloadSubtitleFile
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Owns the on-device cache and Media3 DownloadManager that back every movie/anime download.
 * Mirrors AsterionMac's AVAssetDownloadURLSession-based MediaDownloadManager: one persistent
 * cache directory that both the downloader writes into and offline playback reads from.
 */
class VideoDownloadManager(context: Context, private val downloadIndexStore: DownloadIndexStore, private val httpClient: OkHttpClient) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadDirectory = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "video_downloads")
    private val databaseProvider = StandaloneDatabaseProvider(appContext)

    val downloadCache = SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)

    private val upstreamDataSourceFactory = DefaultDataSource.Factory(
        context,
        DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true),
    )

    val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
        // Offline playback should only ever read what's already downloaded, never silently
        // fall back to a live network fetch that isn't tracked by the download record.
        .setCacheWriteDataSinkFactory(null)

    val downloadManager: DownloadManager = DownloadManager(
        appContext,
        databaseProvider,
        downloadCache,
        upstreamDataSourceFactory,
        Executors.newFixedThreadPool(2),
    ).apply {
        maxParallelDownloads = 2
        addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                scope.launch {
                    downloadIndexStore.update(download.request.id) { existing ->
                        val entry = existing ?: return@update null
                        val phase = when (download.state) {
                            Download.STATE_COMPLETED -> DownloadPhase.COMPLETED
                            Download.STATE_FAILED -> DownloadPhase.FAILED
                            else -> DownloadPhase.DOWNLOADING
                        }
                        entry.copy(
                            phase = phase,
                            progress = when {
                                phase == DownloadPhase.COMPLETED -> 1f
                                download.percentDownloaded >= 0f -> (download.percentDownloaded / 100f).coerceIn(0f, 1f)
                                else -> estimateProgress(download.bytesDownloaded)
                            },
                            localUri = if (phase == DownloadPhase.COMPLETED) entry.id else entry.localUri,
                            errorMessage = finalException?.message,
                        )
                    }
                }
            }
        })
    }

    init {
        DownloadService.sendResumeDownloads(appContext, AsterionDownloadService::class.java, false)
    }

    /** Kicks off the video download and, separately, fetches each subtitle track as its own local .vtt file - matching AsterionMac, which downloads captions independently of the video asset rather than muxing them in. */
    suspend fun startDownload(
        contentType: DownloadContentType,
        contentId: String,
        contentTitle: String,
        unitId: String,
        unitTitle: String?,
        imageUrl: String?,
        source: PlaybackSource,
    ) {
        val id = when (contentType) {
            DownloadContentType.MOVIE -> DownloadEntry.movieId(contentId)
            DownloadContentType.ANIME -> DownloadEntry.animeId(contentId, unitId.toIntOrNull() ?: 0)
            DownloadContentType.NOVEL -> DownloadEntry.novelId(contentId, unitId.toIntOrNull() ?: 0)
        }
        downloadIndexStore.upsert(
            DownloadEntry(
                id = id,
                contentType = contentType,
                contentId = contentId,
                contentTitle = contentTitle,
                unitId = unitId,
                unitTitle = unitTitle,
                imageUrl = imageUrl,
                quality = source.label,
                phase = DownloadPhase.QUEUED,
            ),
        )

        val request = DownloadRequest.Builder(id, Uri.parse(source.uri))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        DownloadService.sendAddDownload(
            appContext,
            AsterionDownloadService::class.java,
            request,
            false,
        )

        if (source.subtitles.isNotEmpty()) {
            scope.launch { downloadSubtitles(id, source) }
        }
    }

    private suspend fun downloadSubtitles(downloadId: String, source: PlaybackSource) {
        val dir = File(downloadDirectory, "subtitles/$downloadId")
        dir.mkdirs()
        val saved = source.subtitles.mapIndexedNotNull { index, track ->
            runCatching {
                val body = httpClient.newCall(Request.Builder().url(track.uri).build()).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    response.body.string()
                }
                val file = File(dir, "$index.vtt")
                file.writeText(body)
                DownloadSubtitleFile(label = track.label, localUri = file.toURI().toString(), language = track.language)
            }.getOrNull()
        }
        if (saved.isNotEmpty()) {
            downloadIndexStore.update(downloadId) { it?.copy(subtitles = saved) }
        }
    }

    fun cancelOrRemove(entry: DownloadEntry) {
        if (entry.contentType == DownloadContentType.NOVEL) {
            entry.localUri?.let { runCatching { File(Uri.parse(it).path ?: return@let).delete() } }
        } else {
            DownloadService.sendRemoveDownload(
                appContext,
                AsterionDownloadService::class.java,
                entry.id,
                false,
            )
            File(downloadDirectory, "subtitles/${entry.id}").deleteRecursively()
        }
        scope.launch { downloadIndexStore.remove(entry.id) }
    }
}

// The anime-scraper's HLS segment proxy serves segments over chunked transfer-encoding with no
// Content-Length header, so Media3 can never learn a download's total size and percentDownloaded
// stays permanently unset (-1) until the moment it completes. Without this, the index reports a
// download as frozen at 0% for its entire (often multi-minute) duration even while it's actively
// writing data, which looks indistinguishable from a hung download. Approximate real, moving
// progress from bytes actually written instead, asymptotically approaching but never reaching
// 100% - only the COMPLETED transition itself reports 1f.
private fun estimateProgress(bytesDownloaded: Long): Float {
    val megabytesDownloaded = bytesDownloaded / (1024f * 1024f)
    return (megabytesDownloaded / (megabytesDownloaded + 20f)).coerceIn(0f, 0.95f)
}
