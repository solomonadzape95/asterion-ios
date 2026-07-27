import 'package:better_player_plus/better_player_plus.dart';
import 'package:flutter/material.dart';

/// De-risking spike (see Phase 1 of the migration plan): proves better_player_plus can
/// handle a real HLS stream from asterion-scraper's proxy, a real WebVTT subtitle track
/// through the same proxy, and Picture-in-Picture, before committing to the full build.
///
/// This exact URL is stale as of 2026-07-21: its segments now resolve to 1x1 placeholder
/// PNGs (nekostream token expired) and asterion-scraper's /api/amp/stream/* endpoint is
/// 502ing backend-side for every title, so there's currently no way to mint a fresh real
/// URL to re-verify end-to-end. That's a backend/upstream problem, not a player bug —
/// see below.
///
/// iOS root-cause status: RULED OUT the leading suspects, not confirmed as still broken.
/// Reproduced our proxy's exact playlist shape locally (no CODECS attribute on the
/// master, extensionless /proxy/ts?url=... segment URLs, raw MPEG-TS segments) via a
/// throwaway local HTTP server and pointed better_player_plus at it on the iOS
/// Simulator: it played cleanly (progressing timecode, no black screen). So the earlier
/// "AVPlayer won't render a frame" symptom was very likely a stale/expired stream token
/// at the moment of that test, not a platform or package limitation. Re-verify against a
/// truly fresh URL once asterion-scraper's stream endpoint recovers before trusting this
/// fully; ideally also test on a real device, not just Simulator.
///
/// Verified live against the real backend before wiring this up:
///   video:    https://asterion-scraper.cyberverse.cloud/proxy/m3u8?url=... -> 200, application/vnd.apple.mpegurl
///   subtitle: https://asterion-scraper.cyberverse.cloud/proxy/subtitle?url=... -> 200, text/vtt
const _spikeVideoUrl =
    'https://asterion-scraper.cyberverse.cloud/proxy/m3u8?url=https%3A%2F%2Fmt.nekostream.site%2F1b252d77b90e951c15c32d17e81e5882%2Fmaster.m3u8';
const _spikeSubtitleUrl =
    'https://asterion-scraper.cyberverse.cloud/proxy/subtitle?url=https%3A%2F%2Fmt.nekostream.site%2F1b252d77b90e951c15c32d17e81e5882%2Fsubtitles%2FEnglish.vtt';

class VideoSpikeScreen extends StatefulWidget {
  const VideoSpikeScreen({super.key});

  @override
  State<VideoSpikeScreen> createState() => _VideoSpikeScreenState();
}

class _VideoSpikeScreenState extends State<VideoSpikeScreen> {
  late final BetterPlayerController _controller;
  final _playerKey = GlobalKey();
  String _status = 'Loading…';

  @override
  void initState() {
    super.initState();

    const configuration = BetterPlayerConfiguration(
      aspectRatio: 16 / 9,
      fit: BoxFit.contain,
      autoPlay: true,
      subtitlesConfiguration: BetterPlayerSubtitlesConfiguration(fontSize: 18, backgroundColor: Colors.transparent),
      controlsConfiguration: BetterPlayerControlsConfiguration(
        enablePip: true,
        enableFullscreen: true,
        enableSubtitles: true,
        enableQualities: true,
        enablePlaybackSpeed: true,
      ),
    );

    final dataSource = BetterPlayerDataSource(
      BetterPlayerDataSourceType.network,
      _spikeVideoUrl,
      // Our proxy URLs are shaped /proxy/m3u8?url=... with no file extension in the
      // path, so better_player_plus's extension-sniffing (Util.inferContentTypeForExtension
      // on the last path segment) throws IndexOutOfBoundsException when there's no "."
      // to split on. Every real video URL from our backends will look like this, so
      // always set the format explicitly rather than relying on auto-detection.
      videoFormat: BetterPlayerVideoFormat.hls,
      subtitles: [
        BetterPlayerSubtitlesSource(
          type: BetterPlayerSubtitlesSourceType.network,
          urls: const [_spikeSubtitleUrl],
          name: 'English',
          selectedByDefault: true,
        ),
      ],
    );

    _controller = BetterPlayerController(configuration);
    _controller.setBetterPlayerGlobalKey(_playerKey);
    _controller.addEventsListener(_onPlayerEvent);
    _controller.setupDataSource(dataSource);
  }

  void _onPlayerEvent(BetterPlayerEvent event) {
    setState(() => _status = event.betterPlayerEventType.name);
  }

  @override
  void dispose() {
    _controller.removeEventsListener(_onPlayerEvent);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Video spike')),
      body: Column(
        children: [
          AspectRatio(
            aspectRatio: 16 / 9,
            child: BetterPlayer(controller: _controller, key: _playerKey),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Last event: $_status'),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 12,
                  children: [
                    ElevatedButton(
                      onPressed: () => _controller.enablePictureInPicture(_playerKey),
                      child: const Text('Enter PiP'),
                    ),
                    ElevatedButton(
                      onPressed: () => _controller.disablePictureInPicture(),
                      child: const Text('Exit PiP'),
                    ),
                    ElevatedButton(
                      onPressed: () {
                        final position = _controller.videoPlayerController?.value.position ?? Duration.zero;
                        _controller.seekTo(position + const Duration(seconds: 10));
                      },
                      child: const Text('+10s'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
