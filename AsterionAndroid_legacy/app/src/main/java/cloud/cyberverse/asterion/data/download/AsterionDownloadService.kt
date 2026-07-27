package cloud.cyberverse.asterion.data.download

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import cloud.cyberverse.asterion.R
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "asterion_downloads"
private const val JOB_ID = 1
private const val FOREGROUND_NOTIFICATION_ID = 2001

/** The foreground service Media3's DownloadManager needs to keep downloading while the app is backgrounded. */
class AsterionDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.downloads_channel_name,
    R.string.downloads_channel_description,
) {
    private val videoDownloadManager: VideoDownloadManager by inject()
    private val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager = videoDownloadManager.downloadManager

    // Lets queued downloads resume after a reboot or once network requirements are met again,
    // even if the app process isn't running - matches AsterionMac's background URLSession downloads.
    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            null,
            downloads,
            notMetRequirements,
        )
}
