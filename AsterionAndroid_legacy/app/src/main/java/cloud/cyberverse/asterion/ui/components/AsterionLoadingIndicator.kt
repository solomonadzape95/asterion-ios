package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An indeterminate loading spinner.
 *
 * This used to pulse a book glyph borrowed from the icon set, which read as a decorative image
 * rather than as progress - it did not rotate, so nothing communicated that work was ongoing, and
 * the same glyph doubled as the app's logo, so a loading screen looked like a splash screen.
 *
 * A sweeping arc is the conventional signal for "working, duration unknown", and it is built here
 * rather than taken from Material so it can carry the app's own stroke weight and accent while
 * staying visually quieter than the stock indicator.
 */
@Composable
fun AsterionLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null,
    size: Dp = 34.dp,
    strokeWidth: Dp = 3.dp,
) {
    val transition = rememberInfiniteTransition(label = "loader")

    // Rotation is constant; the arc's length breathes independently. Together they read as motion
    // with momentum rather than as a rigidly spinning ring.
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "loaderRotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 20f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                20f at 0
                280f at 700
                20f at 1400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "loaderSweep",
    )

    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
    val arcColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // A faint full ring keeps the spinner from looking like a stray mark on the page.
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = arcColor,
                startAngle = rotation,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Centres [AsterionLoadingIndicator] in the space it is given. */
@Composable
fun AsterionLoadingBox(modifier: Modifier = Modifier, label: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsterionLoadingIndicator(label = label)
    }
}
