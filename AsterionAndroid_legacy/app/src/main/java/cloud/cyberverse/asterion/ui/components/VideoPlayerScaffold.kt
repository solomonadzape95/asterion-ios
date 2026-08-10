package cloud.cyberverse.asterion.ui.components

import cloud.cyberverse.asterion.ui.theme.OverlayColors

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val TAG = "VideoPlayerScaffold"
private const val DOUBLE_TAP_SEEK_MS = 10_000L
private const val SEEK_FLASH_DURATION_MS = 650L

// A drag across the full screen width seeks by this many ms per dp - independent of video
// duration so scrub sensitivity stays predictable regardless of how long the title is.
private const val SCRUB_MS_PER_DP = 200L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun enterPictureInPicture(activity: Activity?, player: Player) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val width = player.videoSize.width.takeIf { it > 0 } ?: 16
    val height = player.videoSize.height.takeIf { it > 0 } ?: 9
    // Android clamps to [1/2.39, 2.39]; a portrait-tall video would otherwise throw here.
    val ratio = (width.toFloat() / height.toFloat()).coerceIn(0.42f, 2.39f)
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        .build()
    // Devices/emulator images without FEATURE_PICTURE_IN_PICTURE throw here; logging instead of
    // silently swallowing it is the difference between "PiP doesn't work" being diagnosable.
    runCatching { activity.enterPictureInPictureMode(params) }
        .onFailure { Log.w(TAG, "enterPictureInPictureMode failed", it) }
}

/** Video surface + transport controls: correct letterboxing, subtitles, speed/source, fullscreen, PiP, and gestures. */
@Composable
fun VideoPlayerScaffold(
    player: Player,
    sources: List<PlaybackSource> = emptyList(),
    selectedSourceIndex: Int = 0,
    onSelectSource: (Int) -> Unit = {},
    // Surfaced when every source has failed to play - the caller keeps this Ready/scaffolded
    // (instead of tearing down to a dead-end error screen) specifically so the source picker in
    // VideoPlayerControls below stays reachable without closing and reopening the player.
    playbackError: String? = null,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // LocalContext.current can be a ContextWrapper rather than the Activity itself,
    // so a direct `as? Activity` cast can silently fail here - this walks the chain.
    val activity = LocalContext.current.findActivity()
    val view = LocalView.current
    val density = LocalDensity.current
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val isInPictureInPicture by PictureInPictureController.isInPictureInPicture.collectAsState()

    // Double-tap-to-seek flash: null when idle, otherwise which side was tapped. `seekFlashId` is
    // bumped on every tap so a repeated same-direction tap restarts the fade instead of no-op'ing.
    var seekFlashForward by remember { mutableStateOf<Boolean?>(null) }
    var seekFlashId by remember { mutableLongStateOf(0L) }

    // Drag-to-scrub preview: non-null only while a horizontal drag is in progress.
    var scrubTargetMs by remember { mutableStateOf<Long?>(null) }
    var dragStartPositionMs by remember { mutableLongStateOf(0L) }
    var dragAccumulatedPx by remember { mutableStateOf(0f) }

    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }

    fun applyFullscreen(enabled: Boolean) {
        val window = activity?.window ?: return
        val controller = WindowInsetsControllerCompat(window, view)
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        // A fixed orientation (not SENSOR_LANDSCAPE) so this doesn't depend on accelerometer
        // availability, which some emulators/devices don't reliably report.
        activity.requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(player) {
        PictureInPictureController.registerEnterRequest { enterPictureInPicture(activity, player) }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            PictureInPictureController.registerEnterRequest(null)
            applyFullscreen(false)
        }
    }

    LaunchedEffect(seekFlashId) {
        if (seekFlashForward != null) {
            delay(SEEK_FLASH_DURATION_MS)
            seekFlashForward = null
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    // Mac never stretches video (object-fit: contain) - FIT letterboxes the same way.
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (!isInPictureInPicture) {
            // Placed above the video surface but below VideoPlayerControls below, so control
            // buttons/slider (which consume their own touches first) still win over these
            // gesture detectors - tapping a button never triggers a seek or a visibility toggle.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(player) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                val forward = offset.x > size.width / 2f
                                val target = if (forward) {
                                    (player.currentPosition + DOUBLE_TAP_SEEK_MS).coerceAtMost(duration)
                                } else {
                                    (player.currentPosition - DOUBLE_TAP_SEEK_MS).coerceAtLeast(0L)
                                }
                                player.seekTo(target)
                                seekFlashForward = forward
                                seekFlashId++
                            },
                        )
                    }
                    .pointerInput(player) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartPositionMs = player.currentPosition
                                dragAccumulatedPx = 0f
                                scrubTargetMs = dragStartPositionMs
                            },
                            onDragEnd = {
                                scrubTargetMs?.let { player.seekTo(it) }
                                scrubTargetMs = null
                            },
                            onDragCancel = { scrubTargetMs = null },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatedPx += dragAmount
                                val dragDp = with(density) { dragAccumulatedPx.toDp().value }
                                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                val target = (dragStartPositionMs + (dragDp * SCRUB_MS_PER_DP).toLong())
                                    .coerceIn(0L, duration)
                                scrubTargetMs = target
                            },
                        )
                    },
            )
        }

        seekFlashForward?.let { forward ->
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
                contentAlignment = if (forward) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(OverlayColors.ControlScrim)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (!forward) Icon(PhosphorIcons.FastRewind, contentDescription = null, tint = OverlayColors.Content)
                    Text("10s", color = OverlayColors.Content, modifier = Modifier.padding(horizontal = 4.dp))
                    if (forward) Icon(PhosphorIcons.FastForward, contentDescription = null, tint = OverlayColors.Content)
                }
            }
        }

        if (isBuffering && playbackError == null && !isInPictureInPicture) {
            AsterionLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (playbackError != null && !isInPictureInPicture) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(OverlayColors.PanelScrim)
                        .padding(20.dp),
                ) {
                    Icon(PhosphorIcons.ErrorOutline, contentDescription = null, tint = OverlayColors.Content)
                    Text(
                        playbackError,
                        color = OverlayColors.Content,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        if (sources.size > 1) "Pick another source below, or retry." else "Retry, or go back.",
                        color = OverlayColors.ContentMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OverlayColors.Content),
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Retry") }
                }
            }
        }

        scrubTargetMs?.let { targetMs ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OverlayColors.BubbleScrim)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(formatMillis(targetMs), color = OverlayColors.Content)
                }
            }
        }

        if (!isInPictureInPicture) {
            VideoPlayerControls(
                player = player,
                sources = sources,
                selectedSourceIndex = selectedSourceIndex,
                onSelectSource = onSelectSource,
                isFullscreen = isFullscreen,
                onToggleFullscreen = {
                    isFullscreen = !isFullscreen
                    applyFullscreen(isFullscreen)
                },
                onEnterPictureInPicture = { enterPictureInPicture(activity, player) },
                visible = controlsVisible,
                onVisibleChange = { controlsVisible = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
