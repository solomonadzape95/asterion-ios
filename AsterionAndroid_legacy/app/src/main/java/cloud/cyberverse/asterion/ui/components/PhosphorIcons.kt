package cloud.cyberverse.asterion.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import cloud.cyberverse.asterion.R

/**
 * Phosphor Icons (MIT), from phosphoricons.com, bundled as vector drawables in res/drawable.
 *
 * Phosphor ships no official Compose artifact, so the icons the app actually uses are converted
 * and checked in rather than pulling a large unofficial dependency for a few dozen glyphs. Names
 * here match the call sites they replaced, so `Icons.Filled.Play` reads as `PhosphorIcons.Play`.
 *
 * Each is a @Composable getter because vectorResource needs a composition to resolve resources.
 * They are cheap - the drawable is parsed once and cached by the resource system.
 *
 * To add one: drop the SVG's path data into res/drawable/ph_<name>.xml and add a property here.
 */
object PhosphorIcons {
    val ArrowBack: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_arrow_left)

    val AutoStories: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_book_open_text)

    val Bookmark: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_bookmark_simple_fill)

    val BookmarkBorder: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_bookmark_simple)

    val Check: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_check)

    val CheckCircle: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_check_circle_fill)

    val ChevronLeft: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_caret_left)

    val ChevronRight: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_caret_right)

    val Clear: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_x)

    val ClosedCaption: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_closed_captioning_fill)

    val ClosedCaptionOff: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_closed_captioning)

    val CloudOff: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_cloud_slash)

    val Delete: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_trash)

    val Download: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_download_simple)

    val DownloadDone: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_check_circle)

    val Error: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_warning_circle_fill)

    val ErrorOutline: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_warning_circle)

    val FastForward: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_fast_forward_fill)

    val FastRewind: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_rewind_fill)

    val Fullscreen: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_corners_out)

    val FullscreenExit: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_corners_in)

    val GridView: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_squares_four)

    val JumpTo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_magnifying_glass_plus)

    val Hd: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_high_definition)

    val Home: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_house_fill)

    val KeyboardArrowDown: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_caret_down)

    val KeyboardArrowUp: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_caret_up)

    val List: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_list)

    val LiveTv: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_television_fill)

    val LocalFireDepartment: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_fire_fill)

    val Logout: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_sign_out)

    val MenuBook: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_book_open)

    val Movie: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_film_slate_fill)

    val Palette: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_palette)

    val Pause: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_pause_fill)

    val Person: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_user)

    val PictureInPictureAlt: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_picture_in_picture)

    val PlayArrow: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_play_fill)

    val PlayCircle: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_play_circle_fill)

    val Search: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_magnifying_glass)

    val Settings: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_gear)

    val Shield: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_shield_check)

    val Speed: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_gauge)

    val SportsSoccer: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_soccer_ball_fill)

    val Star: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_star_fill)

    val SwapHoriz: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_arrows_left_right)

    val TextFields: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_text_aa)

    val Visibility: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_eye)
}
