package cloud.cyberverse.asterion.data.download

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** WorkManager builds its own Worker instances by reflection, bypassing Koin - this bridges the two. */
class AsterionWorkerFactory : WorkerFactory(), KoinComponent {
    private val api: AsterionApiService by inject()
    private val downloadIndexStore: DownloadIndexStore by inject()

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        when (workerClassName) {
            NovelDownloadWorker::class.java.name -> NovelDownloadWorker(appContext, workerParameters, api, downloadIndexStore)
            else -> null
        }
}
