package cloud.cyberverse.asterion.ui.theme

import androidx.compose.ui.graphics.Color

// Matches AsterionMac's Support/Theme.swift color(red: 0.612, green: 0.137, blue: 0.208).
val AsterionCrimson = Color(0xFF9C2335)
val AsterionCrimsonDark = Color(0xFFFF7188)

// Material3 assigns every accent role (primaryContainer, secondary, tertiary, ...) its own baseline
// purple by default unless explicitly overridden - these keep every accent role in the same crimson
// family so nothing (chips, selected states, etc.) ever leaks an off-brand colour.
val AsterionLightAccentContainer = Color(0xFFF4D9DC)
val AsterionLightOnAccentContainer = Color(0xFF5C0E1A)
val AsterionDarkAccentContainer = Color(0xFF4A1620)
val AsterionDarkOnAccentContainer = Color(0xFFFFD9DE)

val AsterionLightBackground = Color(0xFFF7F5F1)
val AsterionLightSurface = Color(0xFFFFFFFF)
val AsterionLightCard = Color(0xFFEFEBE3)
val AsterionLightOnBackground = Color(0xFF1A1B20)
val AsterionLightMuted = Color(0xFF6B6560)
val AsterionLightBorder = Color(0xFFDDD7CE)

val AsterionDarkBackground = Color(0xFF121114)
val AsterionDarkSurface = Color(0xFF1C1B1E)
val AsterionDarkCard = Color(0xFF262429)
val AsterionDarkOnBackground = Color(0xFFF0EEEA)
val AsterionDarkMuted = Color(0xFFA8A29E)
val AsterionDarkBorder = Color(0xFF38343A)

val GenreFantasy = Color(0xFF8C6814)
val GenreAction = Color(0xFFA1522E)
val GenreRomance = Color(0xFF8C3A61)
val GenreSciFi = Color(0xFF4A6B8A)
val GenreHorror = Color(0xFF6B3A3A)
val GenreDefault = Color(0xFF3A6B59)

fun genreColor(genres: List<String>?): Color {
    val genre = genres?.firstOrNull()?.lowercase().orEmpty()
    return when {
        genre.contains("fantasy") || genre.contains("xianxia") -> GenreFantasy
        genre.contains("action") || genre.contains("martial") -> GenreAction
        genre.contains("romance") -> GenreRomance
        genre.contains("sci") -> GenreSciFi
        genre.contains("horror") -> GenreHorror
        else -> GenreDefault
    }
}
