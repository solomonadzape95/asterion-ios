package cloud.cyberverse.asterion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cloud.cyberverse.asterion.R
import cloud.cyberverse.asterion.ui.novels.ReaderFont

// AsterionMac reserves Literata (a serif) for titles and reading text, and the system
// sans for UI chrome (buttons, labels, metadata) - mirrored here rather than one font for everything.
private fun literataWeight(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

@OptIn(ExperimentalTextApi::class)
val Literata = FontFamily(
    Font(R.font.literata, weight = FontWeight.Normal, variationSettings = literataWeight(400)),
    Font(R.font.literata, weight = FontWeight.Medium, variationSettings = literataWeight(500)),
    Font(R.font.literata, weight = FontWeight.SemiBold, variationSettings = literataWeight(600)),
    Font(R.font.literata, weight = FontWeight.Bold, variationSettings = literataWeight(700)),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = literataWeight(400),
    ),
)

/** [ReaderFont.SERIF] keeps Asterion's own Literata rather than falling back to the generic system
 * serif - this is the app's default look and the one worth keeping polished; SANS/MONOSPACE give
 * the app-wide font toggle in Profile something real to switch between. */
fun ReaderFont.headingFontFamily(): FontFamily = when (this) {
    ReaderFont.SERIF -> Literata
    ReaderFont.SANS -> FontFamily.SansSerif
    ReaderFont.MONOSPACE -> FontFamily.Monospace
}

/** AsterionMac reserves its display font for titles/reading text and the system sans for UI
 * chrome; [headingFont] swaps only the former (the app-wide font toggle), leaving small chrome
 * roles on the neutral system sans regardless of the chosen font. */
fun asterionTypography(headingFont: FontFamily = Literata): Typography = Typography(
    displayLarge = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = headingFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
