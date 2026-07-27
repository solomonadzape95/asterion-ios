package cloud.cyberverse.asterion.ui.movies

import cloud.cyberverse.asterion.data.model.MovieStreamSource
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import cloud.cyberverse.asterion.ui.components.PlaybackSourceKind

internal const val MOVIES_ORIGIN = "https://asterion-movies.cyberverse.cloud"

/**
 * Use on sources from [cloud.cyberverse.asterion.data.remote.MovieApiService.playback] - see that
 * endpoint's doc comment for why the show endpoint's own `streams` field isn't used for playback.
 *
 * Every source becomes a playable option, not just HLS ones: AsterionMac's
 * MoviePlaybackOption.options(from:) does the same (a non-HLS source becomes a `.web` option
 * opened in a browser view) - filtering to HLS-only here was why Android's source picker showed
 * noticeably fewer options than Mac's for the same title.
 */
fun List<MovieStreamSource>.toPlaybackSources(): List<PlaybackSource> = map { stream ->
    val isDirect = stream.isHls && stream.proxyUrl != null
    PlaybackSource(
        label = "${stream.label} · ${stream.quality}",
        uri = if (isDirect) MOVIES_ORIGIN + stream.proxyUrl else stream.embedUrl,
        kind = if (isDirect) PlaybackSourceKind.DIRECT else PlaybackSourceKind.WEB,
    )
}
