package cloud.cyberverse.asterion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The user-selectable accent.
 *
 * Each entry carries a matched pair rather than one colour used in both themes: the same hue needs
 * to be lighter on a near-black page to stay legible, and darker behind white text on a light one.
 * Every pair clears WCAG AA in both directions - dark >= 4.5:1 against the page, light >= 4.5:1
 * for white text on the fill - so no choice here can produce an unreadable screen.
 */
enum class AccentColor(
    val label: String,
    /** Used in dark theme, sits on the near-black page. */
    val dark: Color,
    /** Used in light theme, carries white text. */
    val light: Color,
    val darkContainer: Color,
    val onDarkContainer: Color,
    val lightContainer: Color,
    val onLightContainer: Color,
) {
    /** The default: warm, legible, and quiet enough to leave cover art as the loudest thing. */
    Gold(
        label = "Gold",
        dark = Color(0xFFD9B25F),
        light = Color(0xFF8A6A22),
        darkContainer = Color(0xFF3A3120),
        onDarkContainer = Color(0xFFF0E2C2),
        lightContainer = Color(0xFFEFE6D2),
        onLightContainer = Color(0xFF453516),
    ),
    Rosewood(
        label = "Rosewood",
        dark = Color(0xFFC98590),
        light = Color(0xFF9E5A64),
        darkContainer = Color(0xFF3A2A2F),
        onDarkContainer = Color(0xFFE9D3D7),
        lightContainer = Color(0xFFEFDFE1),
        onLightContainer = Color(0xFF4A2A31),
    ),
    Sage(
        label = "Sage",
        dark = Color(0xFF9BBE9E),
        light = Color(0xFF4F6E52),
        darkContainer = Color(0xFF27332A),
        onDarkContainer = Color(0xFFD7E6D8),
        lightContainer = Color(0xFFDEE8DF),
        onLightContainer = Color(0xFF26361F),
    ),
    Slate(
        label = "Slate",
        dark = Color(0xFF93AECB),
        light = Color(0xFF4F6785),
        darkContainer = Color(0xFF262F3A),
        onDarkContainer = Color(0xFFD5E1EE),
        lightContainer = Color(0xFFDDE5EE),
        onLightContainer = Color(0xFF22303F),
    ),
    Ember(
        label = "Ember",
        dark = Color(0xFFD79B79),
        light = Color(0xFF8F5533),
        darkContainer = Color(0xFF3A2C23),
        onDarkContainer = Color(0xFFEEDCCF),
        lightContainer = Color(0xFFF0E2D8),
        onLightContainer = Color(0xFF452A18),
    ),
}
