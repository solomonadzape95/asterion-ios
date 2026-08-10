package cloud.cyberverse.asterion.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colours for content drawn **on top of media** - video frames, cover art, poster banners.
 *
 * These deliberately do not follow the theme. A player control or a banner title sits over an
 * arbitrary image whose brightness we do not control, so it needs a fixed light-on-dark treatment
 * to stay legible; switching it to the light theme's dark-on-light would make it invisible over a
 * dark frame. Because they are the one legitimate exception to the colour scheme, they live here
 * as named tokens rather than as `Color.White` scattered across twenty call sites - so the
 * exception is visible, documented, and adjustable in one place.
 *
 * Anything not drawn over media belongs in the colour scheme, not here.
 */
object OverlayColors {
    /** Primary text and icons over media. */
    val Content = Color.White

    /** Secondary text over media - captions, timestamps, source labels. */
    val ContentMuted = Color.White.copy(alpha = 0.70f)

    /** Fill for a control chip resting on media, e.g. a badge on a poster. */
    val ChipSurface = Color.White.copy(alpha = 0.18f)

    /** An inactive dot in a pager indicator sitting over artwork. */
    val InactiveIndicator = Color.White.copy(alpha = 0.40f)

    /** Scrim behind transport controls, so they stay readable over a bright frame. */
    val ControlScrim = Color.Black.copy(alpha = 0.55f)

    /** Heavier scrim for a blocking state over video - an error or a buffering panel. */
    val PanelScrim = Color.Black.copy(alpha = 0.72f)

    /** Scrim for the transient seek/position bubble. */
    val BubbleScrim = Color.Black.copy(alpha = 0.65f)

    /** The dark end of a featured banner's horizontal wash, where its title sits. */
    val BannerScrimStrong = Color.Black.copy(alpha = 0.90f)

    /** The light end of that wash, over the artwork itself. */
    val BannerScrimSoft = Color.Black.copy(alpha = 0.28f)
}
