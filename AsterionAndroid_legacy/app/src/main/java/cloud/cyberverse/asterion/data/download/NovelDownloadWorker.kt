package cloud.cyberverse.asterion.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.local.DownloadEntry
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.data.local.DownloadPhase
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** Downloads one chapter's text to a local file - novels have no video to hand off to Media3, so this runs as a plain WorkManager job. */
class NovelDownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val api: AsterionApiService,
    private val downloadIndexStore: DownloadIndexStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val novelId = inputData.getString(KEY_NOVEL_ID) ?: return Result.failure()
        val novelTitle = inputData.getString(KEY_NOVEL_TITLE).orEmpty()
        val imageUrl = inputData.getString(KEY_IMAGE_URL)
        val chapterNumber = inputData.getInt(KEY_CHAPTER_NUMBER, -1)
        val chapterTitle = inputData.getString(KEY_CHAPTER_TITLE).orEmpty()
        if (chapterNumber < 0) return Result.failure()

        val id = DownloadEntry.novelId(novelId, chapterNumber)
        downloadIndexStore.upsert(
            DownloadEntry(
                id = id,
                contentType = DownloadContentType.NOVEL,
                contentId = novelId,
                contentTitle = novelTitle,
                unitId = chapterNumber.toString(),
                unitTitle = chapterTitle,
                imageUrl = imageUrl,
                phase = DownloadPhase.DOWNLOADING,
            ),
        )

        return try {
            val chapter = api.chapter(novelId, chapterNumber).data
            val dir = File(applicationContext.filesDir, "downloads/novels/$novelId")
            dir.mkdirs()
            val file = File(dir, "$chapterNumber.json")
            file.writeText(json.encodeToString(chapter))
            downloadIndexStore.upsert(
                DownloadEntry(
                    id = id,
                    contentType = DownloadContentType.NOVEL,
                    contentId = novelId,
                    contentTitle = novelTitle,
                    unitId = chapterNumber.toString(),
                    unitTitle = chapter.title,
                    imageUrl = imageUrl,
                    phase = DownloadPhase.COMPLETED,
                    progress = 1f,
                    localUri = file.toURI().toString(),
                ),
            )
            Result.success()
        } catch (error: Exception) {
            downloadIndexStore.upsert(
                DownloadEntry(
                    id = id,
                    contentType = DownloadContentType.NOVEL,
                    contentId = novelId,
                    contentTitle = novelTitle,
                    unitId = chapterNumber.toString(),
                    unitTitle = chapterTitle,
                    imageUrl = imageUrl,
                    phase = DownloadPhase.FAILED,
                    errorMessage = error.message ?: "Download failed",
                ),
            )
            Result.failure()
        }
    }

    companion object {
        const val KEY_NOVEL_ID = "novel_id"
        const val KEY_NOVEL_TITLE = "novel_title"
        const val KEY_IMAGE_URL = "image_url"
        const val KEY_CHAPTER_NUMBER = "chapter_number"
        const val KEY_CHAPTER_TITLE = "chapter_title"
    }
}
