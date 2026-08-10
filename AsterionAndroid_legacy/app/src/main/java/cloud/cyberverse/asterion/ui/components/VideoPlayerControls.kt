package cloud.cyberverse.asterion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import kotlinx.coroutines.delay
import java.util.Locale

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private const val AUTO_HIDE_DELAY_MS = 3000L

internal fun formatMillis(millis: Long): String {
    if (millis < 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

// Matched by label rather than raw list position: the player's own text-track groups can include
// tracks the source's own HLS playlist declares ahead of our sideloaded ones, so the Nth entry in
// `currentSubtitles` is not reliably the Nth TEXT track group on the player.
private fun applySubtitleSelection(player: Player, textGroups: List<Tracks.Group>, track: PlayerSubtitleTrack?) {
    val builder = player.trackSelectionParameters.buildUpon()
    textGroups.forEach { builder.clearOverride(it.mediaTrackGroup) }
    if (track == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    } else {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        val matchedGroup = textGroups.firstOrNull { group ->
            (0 until group.mediaTrackGroup.length).any { i -> group.mediaTrackGroup.getFormat(i).label == track.label }
        }
        matchedGroup?.let { group -> builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, 0)) }
    }
    player.trackSelectionParameters = builder.build()
}

/** A transport bar overlaid on the video surface: play/pause, seek, speed, source, subtitles, fullscreen, PiP. */
@Composable
fun VideoPlayerControls(
    player: Player,
    sources: List<PlaybackSource>,
    selectedSourceIndex: Int,
    onSelectSource: (Int) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var textGroups by remember { mutableStateOf(emptyList<Tracks.Group>()) }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(player.playbackParameters.speed) }

    val currentSubtitles = sources.getOrNull(selectedSourceIndex)?.subtitles.orEmpty()
    // Computed with `remember` (synchronously, during composition) rather than a LaunchedEffect:
    // a LaunchedEffect's body runs in a dispatched coroutine, which can lose the race against
    // DisposableEffect below registering the Player.Listener and the player firing its first
    // onTracksChanged - when that happened, selectedSubtitle below was still null and
    // applySubtitleSelection disabled the text track outright, so captions never rendered even
    // though a default track was picked correctly by the source data.
    var selectedSubtitleIndex by remember(selectedSourceIndex, sources) {
        mutableStateOf(currentSubtitles.indexOfFirst { it.isDefault }.takeIf { it >= 0 })
    }

    // Bumped on every control interaction to restart the auto-hide countdown from that moment.
    var interactionTick by remember { mutableIntStateOf(0) }
    fun keepControlsVisible() {
        onVisibleChange(true)
        interactionTick++
    }

    val selectedSubtitle = selectedSubtitleIndex?.let { currentSubtitles.getOrNull(it) }
    val anyMenuOpen = showSpeedMenu || showSourceMenu || showSubtitleMenu

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                applySubtitleSelection(player, textGroups, selectedSubtitle)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!isSeeking) {
                positionMs = player.currentPosition.coerceAtLeast(0).toFloat()
                durationMs = player.duration.coerceAtLeast(0)
            }
            delay(500)
        }
    }

    // Controls auto-hide after inactivity while playing; paused playback or an open dropdown
    // keeps them on screen indefinitely, matching standard video-player behavior.
    LaunchedEffect(isPlaying, visible, anyMenuOpen, interactionTick) {
        if (isPlaying && visible && !anyMenuOpen) {
            delay(AUTO_HIDE_DELAY_MS)
            onVisibleChange(false)
        }
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    keepControlsVisible()
                    if (isPlaying) player.pause() else player.play()
                }) {
                    Icon(
                        imageVector = if (isPlaying) PhosphorIcons.Pause else PhosphorIcons.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                    )
                }
                Text(formatMillis(positionMs.toLong()), color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                Slider(
                    value = if (durationMs > 0) positionMs / durationMs.toFloat() else 0f,
                    onValueChange = {
                        keepControlsVisible()
                        isSeeking = true
                        positionMs = it * durationMs
                    },
                    onValueChangeFinished = {
                        keepControlsVisible()
                        player.seekTo(positionMs.toLong())
                        isSeeking = false
                    },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
                Text(formatMillis(durationMs), color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { keepControlsVisible(); showSpeedMenu = true }) {
                        Icon(PhosphorIcons.Speed, contentDescription = "Playback speed", tint = Color.White)
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false; keepControlsVisible() }) {
                        PLAYBACK_SPEEDS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (option == 1f) "Normal" else "${option}x") },
                                leadingIcon = { if (option == speed) Icon(PhosphorIcons.Check, contentDescription = null) },
                                onClick = {
                                    keepControlsVisible()
                                    speed = option
                                    player.setPlaybackSpeed(option)
                                    showSpeedMenu = false
                                },
                            )
                        }
                    }
                }

                // Despite the old "HD" icon, these are alternate scraped mirrors/streams for the
                // same title - not real bitrate/resolution variants - so this is labeled "Source".
                if (sources.size > 1) {
                    Box {
                        IconButton(onClick = { keepControlsVisible(); showSourceMenu = true }) {
                            Icon(PhosphorIcons.SwapHoriz, contentDescription = "Source", tint = Color.White)
                        }
                        DropdownMenu(expanded = showSourceMenu, onDismissRequest = { showSourceMenu = false; keepControlsVisible() }) {
                            sources.forEachIndexed { index, source ->
                                DropdownMenuItem(
                                    text = { Text(source.label) },
                                    leadingIcon = { if (index == selectedSourceIndex) Icon(PhosphorIcons.Check, contentDescription = null) },
                                    onClick = {
                                        keepControlsVisible()
                                        onSelectSource(index)
                                        showSourceMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                if (currentSubtitles.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { keepControlsVisible(); showSubtitleMenu = true }) {
                            Icon(
                                if (selectedSubtitleIndex == null) PhosphorIcons.ClosedCaptionOff else PhosphorIcons.ClosedCaption,
                                contentDescription = "Subtitles",
                                tint = Color.White,
                            )
                        }
                        DropdownMenu(expanded = showSubtitleMenu, onDismissRequest = { showSubtitleMenu = false; keepControlsVisible() }) {
                            DropdownMenuItem(
                                text = { Text("Off") },
                                leadingIcon = { if (selectedSubtitleIndex == null) Icon(PhosphorIcons.Check, contentDescription = null) },
                                onClick = {
                                    keepControlsVisible()
                                    selectedSubtitleIndex = null
                                    applySubtitleSelection(player, textGroups, null)
                                    showSubtitleMenu = false
                                },
                            )
                            currentSubtitles.forEachIndexed { index, track ->
                                DropdownMenuItem(
                                    text = { Text(track.label) },
                                    leadingIcon = { if (index == selectedSubtitleIndex) Icon(PhosphorIcons.Check, contentDescription = null) },
                                    onClick = {
                                        keepControlsVisible()
                                        selectedSubtitleIndex = index
                                        applySubtitleSelection(player, textGroups, track)
                                        showSubtitleMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f))

                IconButton(onClick = { keepControlsVisible(); onEnterPictureInPicture() }) {
                    Icon(PhosphorIcons.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
                }
                IconButton(onClick = { keepControlsVisible(); onToggleFullscreen() }) {
                    Icon(
                        imageVector = if (isFullscreen) PhosphorIcons.FullscreenExit else PhosphorIcons.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
