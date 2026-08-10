package cloud.cyberverse.asterion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Softer than Material's defaults across the board. The reference screens read as premium partly
 * because nothing is sharp: cards are generously rounded and every button is a full pill.
 */
val AsterionShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val CoverCornerRadius = 14.dp

/** Buttons, chips, and search fields. Radius tracks height, so these stay true pills at any size. */
val PillShape = RoundedCornerShape(percent = 50)

/** Inputs sit slightly squarer than buttons so a field never reads as something tappable. */
val InputShape = RoundedCornerShape(18.dp)

/** Standard height for primary and secondary actions - comfortably above the 48dp minimum. */
val ButtonHeight = 56.dp
