import 'package:flutter/material.dart';

/// Ported 1:1 from AsterionAndroid's theme/Color.kt, which itself matches
/// AsterionMac's Support/Theme.swift color(red: 0.612, green: 0.137, blue: 0.208).
class AsterionColors {
  AsterionColors._();

  static const crimson = Color(0xFF9C2335);
  static const crimsonDark = Color(0xFFFF7188);

  // Material's ColorScheme assigns every accent role (primaryContainer, secondary,
  // tertiary, ...) its own default purple unless explicitly overridden - keep every
  // accent role in the same crimson family so nothing leaks an off-brand colour.
  static const lightAccentContainer = Color(0xFFF4D9DC);
  static const lightOnAccentContainer = Color(0xFF5C0E1A);
  static const darkAccentContainer = Color(0xFF4A1620);
  static const darkOnAccentContainer = Color(0xFFFFD9DE);

  static const lightBackground = Color(0xFFF7F5F1);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightCard = Color(0xFFEFEBE3);
  static const lightOnBackground = Color(0xFF1A1B20);
  static const lightMuted = Color(0xFF6B6560);
  static const lightBorder = Color(0xFFDDD7CE);

  static const darkBackground = Color(0xFF121114);
  static const darkSurface = Color(0xFF1C1B1E);
  static const darkCard = Color(0xFF262429);
  static const darkOnBackground = Color(0xFFF0EEEA);
  static const darkMuted = Color(0xFFA8A29E);
  static const darkBorder = Color(0xFF38343A);

  static const genreFantasy = Color(0xFF8C6814);
  static const genreAction = Color(0xFFA1522E);
  static const genreRomance = Color(0xFF8C3A61);
  static const genreSciFi = Color(0xFF4A6B8A);
  static const genreHorror = Color(0xFF6B3A3A);
  static const genreDefault = Color(0xFF3A6B59);

  static Color genreColor(List<String>? genres) {
    final genre = (genres?.isNotEmpty == true ? genres!.first : '').toLowerCase();
    if (genre.contains('fantasy') || genre.contains('xianxia')) return genreFantasy;
    if (genre.contains('action') || genre.contains('martial')) return genreAction;
    if (genre.contains('romance')) return genreRomance;
    if (genre.contains('sci')) return genreSciFi;
    if (genre.contains('horror')) return genreHorror;
    return genreDefault;
  }
}
