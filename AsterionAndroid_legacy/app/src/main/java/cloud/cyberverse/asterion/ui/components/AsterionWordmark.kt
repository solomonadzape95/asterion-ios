package cloud.cyberverse.asterion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * The app's wordmark.
 *
 * This previously set a generic book glyph from the icon set beside the name, which was not a
 * logo - it was the same icon used for "open a chapter" and, until recently, for the loading
 * spinner, so the brand mark, a UI affordance, and a progress indicator were all the same picture.
 *
 * A wordmark is the name, set deliberately: heavy weight, wide tracking, with a single accent
 * terminal. It carries no meaning that has to be relearned, and it cannot be confused with a
 * control.
 */
@Composable
fun AsterionWordmark(modifier: Modifier = Modifier, showTagline: Boolean = false) {
    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append("ASTERION")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(".")
                }
            },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
            ),
        )
        if (showTagline) {
            Text(
                "Stories that transcend time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
