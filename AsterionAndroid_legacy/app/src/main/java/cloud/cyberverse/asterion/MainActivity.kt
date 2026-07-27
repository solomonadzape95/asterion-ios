package cloud.cyberverse.asterion

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cloud.cyberverse.asterion.ui.AsterionRoot
import cloud.cyberverse.asterion.ui.components.PictureInPictureController
import cloud.cyberverse.asterion.ui.theme.AsterionTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Without this, download-progress notifications silently never show on Android 13+ -
        // the foreground service itself still runs fine either way.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AsterionTheme {
                AsterionRoot()
            }
        }
    }

    // The user swiping home while a video is playing is exactly when Picture-in-Picture
    // should kick in, mirroring how every other video app on the platform behaves.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PictureInPictureController.requestEnter()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PictureInPictureController.setInPictureInPicture(isInPictureInPictureMode)
    }
}
