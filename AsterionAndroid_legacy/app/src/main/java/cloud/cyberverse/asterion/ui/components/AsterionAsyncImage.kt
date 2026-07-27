package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * A poster/cover image with a shimmering placeholder instead of a flat blank block while the
 * network fetch is in flight - scraped source images (e.g. football match posters) can be slow.
 */
@Composable
fun AsterionAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    var isLoading by remember(model) { mutableStateOf(true) }
    // Coil's AsyncImage treats a new ImageRequest instance as a new request regardless of whether
    // its contents are unchanged (ImageRequest has no structural equals) - without this remember,
    // every unrelated recomposition of an ancestor (scrolling, a nearby animation tick, tab
    // switches) rebuilds the request and makes Coil redo work for every visible image on screen.
    val request = remember(model) { ImageRequest.Builder(context).data(model).crossfade(300).build() }

    Box(modifier) {
        if (isLoading) {
            ShimmerPlaceholder(Modifier.fillMaxSize())
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onState = { state -> isLoading = state is AsyncImagePainter.State.Loading },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
}
