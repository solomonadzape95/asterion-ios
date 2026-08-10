package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.theme.CoverCornerRadius

/**
 * Placeholder shapes in the layout the real content will occupy.
 *
 * A centred spinner tells you nothing except "wait", and when content arrives the whole screen
 * changes at once. Blocks in the right places make the wait feel shorter and stop the layout
 * jumping when data lands.
 */
@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
}

/** Matches the shape of a catalog grid: rows of posters with a title and subtitle beneath. */
@Composable
fun CatalogSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    columns: Int = 3,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SkeletonBlock(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.large),
        )
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(columns) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(CoverCornerRadius)),
                        )
                        SkeletonBlock(Modifier.fillMaxWidth().height(11.dp).clip(MaterialTheme.shapes.extraSmall))
                        SkeletonBlock(Modifier.width(48.dp).height(9.dp).clip(MaterialTheme.shapes.extraSmall))
                    }
                }
            }
        }
    }
}

/** Matches a detail screen: banner, then metadata chips, then an action row. */
@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(300.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            repeat(3) {
                SkeletonBlock(
                    Modifier
                        .width(74.dp)
                        .height(28.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)),
                )
            }
        }
        SkeletonBlock(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)),
        )
        repeat(4) {
            SkeletonBlock(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(13.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
