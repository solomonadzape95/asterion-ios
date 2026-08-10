package cloud.cyberverse.asterion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.vectorResource
import cloud.cyberverse.asterion.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Icons are held as drawable ids rather than ImageVectors because resolving a vector resource
 * needs a composition, which an enum constructor does not have. [icon] does the lookup at render
 * time instead.
 */
enum class AsterionTab(val route: String, val label: String, @DrawableRes val iconRes: Int) {
    Home("home", "Home", R.drawable.ph_house_fill),
    Novels("novels", "Novels", R.drawable.ph_book_open),
    Anime("anime", "Anime", R.drawable.ph_television_fill),
    Movies("movies", "Movies", R.drawable.ph_film_slate_fill),
    Football("football", "Football", R.drawable.ph_soccer_ball_fill),
    Profile("profile", "Profile", R.drawable.ph_user);

    val icon: ImageVector
        @Composable get() = ImageVector.vectorResource(iconRes)
}

/**
 * A floating pill nav bar. Unselected tabs are compact icon-only circles; the active tab expands
 * into a wider pill carrying its label, so it's always legible which tab you're on without every
 * tab fighting for label space at once.
 */
@Composable
fun AsterionBottomBar(currentTab: AsterionTab?, onTabSelected: (AsterionTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsterionTab.entries.forEach { tab ->
                val selected = tab == currentTab
                BottomBarItem(
                    tab = tab,
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    // Every item shares the row via weight (never sized purely to content) so the
                    // bar can never overflow past the screen edge, whichever tab is expanded.
                    modifier = Modifier.weight(if (selected) 2.4f else 1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(tab: AsterionTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "tab-background",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tab-tint",
    )
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = if (selected) 10.dp else 12.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = if (selected) null else tab.label, tint = tint, modifier = Modifier.size(22.dp))
        if (selected) {
            Text(
                tab.label,
                color = tint,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
