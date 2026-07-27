package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.theme.CoverCornerRadius

/** A poster + title/subtitle, matching the shelf cards in the Asterion design reference. */
@Composable
fun CoverCard(
    imageUrl: String?,
    title: String,
    subtitle: String? = null,
    width: androidx.compose.ui.unit.Dp = 120.dp,
    aspectRatio: Float = 2f / 3f,
    topStartBadge: String? = null,
    topEndBadge: String? = null,
    topEndBadgeColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, tween(120), label = "cover-press-scale")

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Box {
            AsterionAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(width)
                    .aspectRatio(aspectRatio)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(CoverCornerRadius), clip = false)
                    .clip(RoundedCornerShape(CoverCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            topStartBadge?.let { CoverBadge(it, Alignment.TopStart, MaterialTheme.colorScheme.primary) }
            topEndBadge?.let { CoverBadge(it, Alignment.TopEnd, topEndBadgeColor ?: MaterialTheme.colorScheme.primary) }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoxScope.CoverBadge(text: String, alignment: Alignment, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .align(alignment)
            .padding(6.dp)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}
