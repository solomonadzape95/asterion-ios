import 'package:flutter/material.dart';

import 'features/video_spike/video_spike_screen.dart';
import 'theme/asterion_theme.dart';

void main() {
  runApp(const AsterionApp());
}

class AsterionApp extends StatelessWidget {
  const AsterionApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Asterion',
      debugShowCheckedModeBanner: false,
      theme: AsterionTheme.light,
      darkTheme: AsterionTheme.dark,
      // Video playback spike is the current entry point while Phase 1 (Novels) is
      // still being scaffolded - see the migration plan's phased build order.
      home: const VideoSpikeScreen(),
    );
  }
}
