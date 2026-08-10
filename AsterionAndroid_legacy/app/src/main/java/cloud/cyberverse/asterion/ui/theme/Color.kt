package cloud.cyberverse.asterion.ui.theme

import androidx.compose.ui.graphics.Color

// A muted rosewood rather than a saturated crimson. The old dark accent (0xFFFF7188) was a fully
// saturated pink against a near-black page - loud enough that it pulled attention away from cover
// art and headings, which are what should carry a reading app. These sit in the same family, just
// desaturated, and both clear WCAG AA: 6.6:1 on the dark page, 5.1:1 for white text on the light
// fill.
val AsterionCrimson = Color(0xFF9E5A64)
val AsterionCrimsonDark = Color(0xFFC98590)

// Material3 assigns every accent role (primaryContainer, secondary, tertiary, ...) its own baseline
// purple by default unless explicitly overridden - these keep every accent role in the same crimson
// family so nothing (chips, selected states, etc.) ever leaks an off-brand colour.
val AsterionLightAccentContainer = Color(0xFFEFDFE1)
val AsterionLightOnAccentContainer = Color(0xFF4A2A31)
val AsterionDarkAccentContainer = Color(0xFF3A2A2F)
val AsterionDarkOnAccentContainer = Color(0xFFE9D3D7)

val AsterionLightBackground = Color(0xFFF7F5F1)
val AsterionLightSurface = Color(0xFFFFFFFF)
val AsterionLightCard = Color(0xFFEFEBE3)
val AsterionLightOnBackground = Color(0xFF1A1B20)
val AsterionLightMuted = Color(0xFF6B6560)
val AsterionLightBorder = Color(0xFFDDD7CE)

// Dark is the app's real home, and depth here comes from stacked values rather than outlines:
// page, then a lifted surface, then a card on top of that. Layering values instead of drawing
// borders is what separates a flat dark theme from one that reads as deliberate.
val AsterionDarkBackground = Color(0xFF0F0E10)
val AsterionDarkSurface = Color(0xFF17161A)
val AsterionDarkCard = Color(0xFF1F1E23)
/** One step above card, for controls sitting on a card - toggles, chips, secondary buttons. */
val AsterionDarkElevated = Color(0xFF272630)
val AsterionDarkOnBackground = Color(0xFFF2F0EE)
val AsterionDarkMuted = Color(0xFFA09B99)
// Hairline only. Borders separate where a value step cannot; they never carry the depth.
val AsterionDarkBorder = Color(0xFF322E36)

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
