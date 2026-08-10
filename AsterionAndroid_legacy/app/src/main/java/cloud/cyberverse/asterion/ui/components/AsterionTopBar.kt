package cloud.cyberverse.asterion.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

/**
 * The one top bar every screen should use. Plain [TopAppBar] defaults to a tonal
 * `surfaceContainer` fill that reads as a different colour from the plain-background content
 * beneath it - this pins the container colour to the background explicitly so the bar always
 * blends into the screen instead of banding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterionTopBar(
    title: String?,
    onBack: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
) {
    AsterionTopBar(
        onBack = onBack,
        containerColor = containerColor,
        contentColor = contentColor,
        actions = actions,
        title = {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterionTopBar(
    onBack: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(PhosphorIcons.ArrowBack, contentDescription = "Back", tint = contentColor)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
        ),
    )
}
