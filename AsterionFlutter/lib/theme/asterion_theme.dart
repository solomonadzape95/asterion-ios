import 'package:flutter/material.dart';

import 'asterion_colors.dart';

/// Mirrors AsterionAndroid's Theme.kt/Type.kt: Literata (serif) for display/reading
/// text, system sans for UI chrome, one crimson accent across every Material color role.
class AsterionTheme {
  AsterionTheme._();

  static final ColorScheme _light = ColorScheme.fromSeed(
    seedColor: AsterionColors.crimson,
    brightness: Brightness.light,
  ).copyWith(
    primary: AsterionColors.crimson,
    onPrimary: AsterionColors.lightSurface,
    primaryContainer: AsterionColors.lightAccentContainer,
    onPrimaryContainer: AsterionColors.lightOnAccentContainer,
    secondary: AsterionColors.crimson,
    onSecondary: AsterionColors.lightSurface,
    secondaryContainer: AsterionColors.lightAccentContainer,
    onSecondaryContainer: AsterionColors.lightOnAccentContainer,
    tertiary: AsterionColors.crimson,
    onTertiary: AsterionColors.lightSurface,
    tertiaryContainer: AsterionColors.lightAccentContainer,
    onTertiaryContainer: AsterionColors.lightOnAccentContainer,
    surface: AsterionColors.lightSurface,
    onSurface: AsterionColors.lightOnBackground,
    surfaceContainerHighest: AsterionColors.lightCard,
    onSurfaceVariant: AsterionColors.lightMuted,
    outline: AsterionColors.lightBorder,
  );

  static final ColorScheme _dark = ColorScheme.fromSeed(
    seedColor: AsterionColors.crimsonDark,
    brightness: Brightness.dark,
  ).copyWith(
    primary: AsterionColors.crimsonDark,
    onPrimary: AsterionColors.darkBackground,
    primaryContainer: AsterionColors.darkAccentContainer,
    onPrimaryContainer: AsterionColors.darkOnAccentContainer,
    secondary: AsterionColors.crimsonDark,
    onSecondary: AsterionColors.darkBackground,
    secondaryContainer: AsterionColors.darkAccentContainer,
    onSecondaryContainer: AsterionColors.darkOnAccentContainer,
    tertiary: AsterionColors.crimsonDark,
    onTertiary: AsterionColors.darkBackground,
    tertiaryContainer: AsterionColors.darkAccentContainer,
    onTertiaryContainer: AsterionColors.darkOnAccentContainer,
    surface: AsterionColors.darkSurface,
    onSurface: AsterionColors.darkOnBackground,
    surfaceContainerHighest: AsterionColors.darkCard,
    onSurfaceVariant: AsterionColors.darkMuted,
    outline: AsterionColors.darkBorder,
  );

  static ThemeData get light => _build(_light, AsterionColors.lightBackground);
  static ThemeData get dark => _build(_dark, AsterionColors.darkBackground);

  static ThemeData _build(ColorScheme scheme, Color background) {
    final displayStyle = const TextStyle(fontFamily: 'Literata', fontWeight: FontWeight.w600);
    final readingStyle = const TextStyle(fontFamily: 'Literata', fontWeight: FontWeight.w400);

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      textTheme: TextTheme(
        displayLarge: displayStyle.copyWith(fontSize: 34, height: 40 / 34),
        displayMedium: displayStyle.copyWith(fontSize: 28, height: 34 / 28),
        headlineLarge: displayStyle.copyWith(fontSize: 24, height: 30 / 24),
        headlineMedium: displayStyle.copyWith(fontSize: 20, height: 26 / 20),
        titleLarge: displayStyle.copyWith(fontSize: 18, height: 24 / 18),
        titleMedium: const TextStyle(fontWeight: FontWeight.w500, fontSize: 16, height: 22 / 16),
        titleSmall: const TextStyle(fontWeight: FontWeight.w500, fontSize: 14, height: 20 / 14),
        bodyLarge: readingStyle.copyWith(fontSize: 17, height: 27 / 17),
        bodyMedium: readingStyle.copyWith(fontSize: 15, height: 23 / 15),
        bodySmall: const TextStyle(fontSize: 13, height: 18 / 13),
        labelLarge: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, height: 20 / 14),
        labelMedium: const TextStyle(fontWeight: FontWeight.w500, fontSize: 12, height: 16 / 12),
        labelSmall: const TextStyle(fontWeight: FontWeight.w500, fontSize: 11, height: 14 / 11),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        surfaceTintColor: Colors.transparent,
        foregroundColor: scheme.onSurface,
        elevation: 0,
      ),
    );
  }
}
