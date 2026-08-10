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

private fun weightAxis(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

/**
 * Inter carries the interface: headings, chrome, buttons, metadata.
 *
 * The reference screens get their premium feel largely from type - very bold, tightly tracked
 * headlines against muted body text - and Inter is built for exactly that at small sizes on screen.
 */
@OptIn(ExperimentalTextApi::class)
val Inter = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = weightAxis(400)),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = weightAxis(500)),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = weightAxis(600)),
    Font(R.font.inter, weight = FontWeight.Bold, variationSettings = weightAxis(700)),
    Font(R.font.inter, weight = FontWeight.ExtraBold, variationSettings = weightAxis(800)),
)

/**
 * Literata stays for long-form reading, where a serif genuinely reads better than a UI sans, and
 * remains selectable app-wide for readers who prefer it.
 */
@OptIn(ExperimentalTextApi::class)
val Literata = FontFamily(
    Font(R.font.literata, weight = FontWeight.Normal, variationSettings = weightAxis(400)),
    Font(R.font.literata, weight = FontWeight.Medium, variationSettings = weightAxis(500)),
    Font(R.font.literata, weight = FontWeight.SemiBold, variationSettings = weightAxis(600)),
    Font(R.font.literata, weight = FontWeight.Bold, variationSettings = weightAxis(700)),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = weightAxis(400),
    ),
)

fun ReaderFont.headingFontFamily(): FontFamily = when (this) {
    ReaderFont.SERIF -> Literata
    ReaderFont.SANS -> Inter
    ReaderFont.MONOSPACE -> FontFamily.Monospace
}

/**
 * One family across the whole scale, with weight and tracking doing the work instead of a
 * serif/sans split. Headlines are heavy and negatively tracked; body and label roles stay at
 * comfortable reading weights.
 *
 * [headingFont] is the app-wide font toggle in Profile and swaps only the display/headline/title
 * roles, so choosing the serif does not turn buttons and metadata into serif too.
 */
fun asterionTypography(headingFont: FontFamily = Inter): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Previously undefined while still being used, so featured banner titles silently fell back to
    // the Material baseline in the system font.
    headlineSmall = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = headingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
