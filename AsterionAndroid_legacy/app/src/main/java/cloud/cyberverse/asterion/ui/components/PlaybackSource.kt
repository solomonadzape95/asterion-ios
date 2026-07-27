package cloud.cyberverse.asterion.ui.components

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

data class PlayerSubtitleTrack(val label: String, val uri: String, val language: String?, val isDefault: Boolean = false)

/** DIRECT plays natively through ExoPlayer (an HLS/mp4 URL); WEB has no such URL - it's an
 * embed/iframe page that only a browser engine can render, so it's shown in a WebView instead.
 * Mirrors AsterionMac's MoviePlaybackOption.Kind. */
enum class PlaybackSourceKind { DIRECT, WEB }

/** One playable rendition of a title/episode - a "quality" in the player UI is really just picking one of these. */
data class PlaybackSource(
    val label: String,
    val uri: String,
    val subtitles: List<PlayerSubtitleTrack> = emptyList(),
    val kind: PlaybackSourceKind = PlaybackSourceKind.DIRECT,
)

fun PlaybackSource.toMediaItem(mimeType: String = MimeTypes.APPLICATION_M3U8): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMimeType(mimeType)
        .setSubtitleConfigurations(
            subtitles.map { track ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.uri))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(track.language ?: "und")
                    .setLabel(track.label)
                    .setSelectionFlags(if (track.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            },
        )
        .build()
