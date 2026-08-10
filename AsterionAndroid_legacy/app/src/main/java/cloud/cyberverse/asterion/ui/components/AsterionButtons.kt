package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.theme.ButtonHeight
import cloud.cyberverse.asterion.ui.theme.PillShape

/**
 * Press feedback, physics rather than a timed curve.
 *
 * A spring settles the way a physical object does; a tween of fixed duration always feels
 * mechanical by comparison. This is the app's baseline interaction and everything tappable that
 * needs a press response should use it.
 */
@Composable
private fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return scale(scale)
}

/**
 * The primary action: a full-height pill with a light fill and dark text.
 *
 * Inverting the fill this way - rather than the usual dark-button-on-dark-page - is what makes the
 * primary action unmissable on a near-black screen, and is the single most recognisable move in
 * the reference designs.
 */
@Composable
fun AsterionFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .height(ButtonHeight)
            .defaultMinSize(minWidth = 120.dp)
            .pressScale(interactionSource),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/**
 * The secondary action: same pill, filled with the elevated surface rather than outlined.
 *
 * A hairline outline on a near-black page reads as a disabled control; a value step reads as a
 * real, tappable thing.
 */
@Composable
fun AsterionOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        interactionSource = interactionSource,
        border = null,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier
            .height(ButtonHeight)
            .defaultMinSize(minWidth = 120.dp)
            .pressScale(interactionSource),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 0.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * A square-ish pill for a secondary action that stands beside a primary button - save, download,
 * share. Matches [AsterionOutlinedButton]'s surface so a row of actions reads as one control group.
 *
 * [selected] flips it to the accent, for actions with an on/off state like bookmarking.
 */
@Composable
fun AsterionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconButtonContainer",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconButtonTint",
    )

    Box(
        modifier = modifier
            .size(ButtonHeight)
            .pressScale(interactionSource)
            .clip(PillShape)
            .background(container)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}
