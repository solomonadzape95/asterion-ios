package cloud.cyberverse.asterion.ui.anime

import cloud.cyberverse.asterion.data.model.AnimeStreamSource
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import cloud.cyberverse.asterion.ui.components.PlayerSubtitleTrack
import java.net.URLEncoder

private const val ANIME_SCRAPER_ORIGIN = "https://asterion-scraper.cyberverse.cloud"

// The scraper proxies most subtitle files to add the CDN's expected headers, and returns that
// proxy path as origin-relative (e.g. "/proxy/subtitle?url=..."). AsterionMac resolves this
// against its base URL (AnimeAPI.serviceURL) before use; Android needs the same resolution or
// ExoPlayer gets handed a schemeless URI it silently fails to load, so captions never render
// even though the track shows up in the picker.
private fun resolvedSubtitleUrl(fileUrl: String) = if (fileUrl.startsWith("http")) fileUrl else "$ANIME_SCRAPER_ORIGIN$fileUrl"

fun List<AnimeStreamSource>.playbackSources(): List<PlaybackSource> =
    filter { !it.directUrl.isNullOrBlank() }.map { stream ->
        // The video CDN 403s direct requests (anti-hotlink); the scraper's own proxy
        // adds the Referer/User-Agent it expects, mirroring AsterionMac's playableDirectURL.
        val encoded = URLEncoder.encode(stream.directUrl, "UTF-8")
        val playableUrl = "$ANIME_SCRAPER_ORIGIN/proxy/m3u8?url=$encoded"
        PlaybackSource(
            // `quality` is frequently the same generic placeholder ("HD") on every scraped
            // mirror, which is exactly what made the picker show a wall of identical "HD"
            // entries - `server` (e.g. "HD-1", "HD-2") is the field that actually tells mirrors
            // apart, so it takes priority; `quality` is only appended when it adds information.
            label = if (stream.quality != null && stream.quality != stream.server) "${stream.server} · ${stream.quality}" else stream.server,
            uri = playableUrl,
            subtitles = stream.tracks.map { track ->
                PlayerSubtitleTrack(
                    label = track.label ?: "Subtitles",
                    uri = resolvedSubtitleUrl(track.fileUrl),
                    language = track.languageCode,
                    isDefault = track.isDefault,
                )
            },
        )
    }
