package cloud.cyberverse.asterion.data.download

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cloud.cyberverse.asterion.data.model.Novel

class NovelDownloadRepository(private val context: Context) {
    fun downloadChapter(novel: Novel, chapterNumber: Int, chapterTitle: String) {
        val input = Data.Builder()
            .putString(NovelDownloadWorker.KEY_NOVEL_ID, novel.id)
            .putString(NovelDownloadWorker.KEY_NOVEL_TITLE, novel.title)
            .putString(NovelDownloadWorker.KEY_IMAGE_URL, novel.imageUrl)
            .putInt(NovelDownloadWorker.KEY_CHAPTER_NUMBER, chapterNumber)
            .putString(NovelDownloadWorker.KEY_CHAPTER_TITLE, chapterTitle)
            .build()
        val request = OneTimeWorkRequestBuilder<NovelDownloadWorker>().setInputData(input).build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
