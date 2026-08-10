package cloud.cyberverse.asterion.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale

/**
 * A header built from the artwork itself: the cover blown up, blurred, and faded into the page,
 * with [content] laid over it.
 *
 * Two things make this read as intentional rather than as a big fuzzy image:
 *
 * - The scrim is a three-stop gradient that lands on the page background exactly at the bottom
 *   edge, so the banner dissolves into the screen instead of ending on a visible seam.
 * - The artwork is dimmed before it is blurred. Cover art is often highly saturated, and a bright
 *   blur behind text makes the text unreadable no matter how heavy the scrim.
 *
 * `Modifier.blur` needs API 31. Below that it is a silent no-op, which would leave a sharp,
 * stretched cover behind the title - worse than no banner. So on older devices the image is
 * deliberately decoded tiny and scaled up, which produces a genuine soft blur for free.
 */
@Composable
fun BlurredArtworkBanner(
    model: Any?,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    blurRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val background = MaterialTheme.colorScheme.background

    Box(modifier.fillMaxWidth().height(height)) {
        if (model != null) {
            val request = ImageRequest.Builder(context)
                .data(model)
                .crossfade(400)
                .apply {
                    if (!supportsBlur) {
                        // Decoding at a fraction of the display size and letting it scale up is a
                        // real blur, not an approximation of one.
                        size(48, 72)
                        scale(Scale.FILL)
                    }
                }
                .build()

            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (supportsBlur) Modifier.blur(blurRadius) else Modifier),
            )
        }

        // Dim first, then wash to the page colour. Ordering matters: the dim is what keeps bright
        // cover art from bleeding through the title.
        Box(Modifier.fillMaxSize().background(background.copy(alpha = 0.35f)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0.10f),
                    0.45f to background.copy(alpha = 0.55f),
                    0.85f to background.copy(alpha = 0.94f),
                    1f to background,
                ),
            ),
        )

        content()
    }
}
