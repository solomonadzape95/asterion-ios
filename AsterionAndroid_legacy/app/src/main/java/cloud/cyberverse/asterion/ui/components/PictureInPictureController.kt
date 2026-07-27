package cloud.cyberverse.asterion.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridges MainActivity's PiP lifecycle callbacks (onUserLeaveHint, onPictureInPictureModeChanged)
 * to whichever VideoPlayerScaffold is currently on screen, without every screen needing its own
 * Activity reference wiring - only one player is ever visible at a time.
 */
object PictureInPictureController {
    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture

    private var enterRequest: (() -> Unit)? = null

    fun registerEnterRequest(request: (() -> Unit)?) {
        enterRequest = request
    }

    fun requestEnter() {
        enterRequest?.invoke()
    }

    fun setInPictureInPicture(value: Boolean) {
        _isInPictureInPicture.value = value
    }
}
