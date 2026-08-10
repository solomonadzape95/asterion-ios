package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.theme.PillShape
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val THUMB_MIN_FRACTION = 0.06f
private const val AUTO_HIDE_DELAY_MS = 1200L

/**
 * A draggable scroll thumb for very long lists, with a label showing where the drag will land.
 *
 * A four-thousand-chapter list is roughly a hundred screens. Flinging through that is not
 * navigation, it is endurance, and Compose's lazy lists ship no scrollbar of their own. Dragging
 * this crosses the whole list in one gesture.
 *
 * Appears while scrolling or dragging and fades out shortly after, so it never sits over content
 * that is being read.
 */
@Composable
fun BoxScope.FastScrollbar(
    state: LazyGridState,
    totalItems: Int,
    modifier: Modifier = Modifier,
    label: (Int) -> String = { "" },
) {
    if (totalItems <= 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val isScrolling = state.isScrollInProgress
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            isVisible = true
        } else {
            delay(AUTO_HIDE_DELAY_MS)
            isVisible = false
        }
    }

    // While dragging, the thumb follows the finger rather than the list, so it can't fight the
    // scroll it is causing.
    val scrollFraction = if (isDragging) {
        dragFraction
    } else {
        val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val maxIndex = (totalItems - visible).coerceAtLeast(1)
        (state.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
    }

    val thumbFraction = remember(totalItems, state.layoutInfo.visibleItemsInfo.size) {
        val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        (visible.toFloat() / totalItems).coerceIn(THUMB_MIN_FRACTION, 1f)
    }

    fun scrollTo(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * (totalItems - 1)).roundToInt()
        scope.launch { state.scrollToItem(target.coerceIn(0, totalItems - 1)) }
    }

    AnimatedVisibility(
        visible = isVisible && thumbFraction < 1f,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessLow)),
        modifier = modifier.align(Alignment.CenterEnd),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 6.dp)
                .width(28.dp),
        ) {
            val trackHeight = maxHeight
            trackHeightPx = with(density) { trackHeight.toPx() }
            val thumbHeight = trackHeight * thumbFraction
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .pointerInput(totalItems, trackHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                if (trackHeightPx > 0f) {
                                    dragFraction = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                    scrollTo(dragFraction)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                        ) { change, dragAmount ->
                            change.consume()
                            if (trackHeightPx > 0f) {
                                dragFraction = (dragFraction + dragAmount / trackHeightPx).coerceIn(0f, 1f)
                                scrollTo(dragFraction)
                            }
                        }
                    },
            ) {
                val offsetY = (trackHeight - thumbHeight) * scrollFraction

                Box(
                    Modifier
                        .padding(top = offsetY)
                        .align(Alignment.TopEnd)
                        .width(if (isDragging) 8.dp else 5.dp)
                        .height(thumbHeight)
                        .clip(PillShape)
                        .background(
                            if (isDragging) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            },
                        ),
                )
            }

            // Only while dragging: a permanent bubble would cover the content it describes.
            if (isDragging) {
                val target = (dragFraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
                val text = label(target)
                if (text.isNotEmpty()) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = ((trackHeight - thumbHeight) * scrollFraction), end = 34.dp)
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
