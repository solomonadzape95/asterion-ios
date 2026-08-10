package cloud.cyberverse.asterion.ui.novels

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.model.Chapter
import cloud.cyberverse.asterion.ui.components.PhosphorIcons
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import cloud.cyberverse.asterion.ui.theme.InputShape
import cloud.cyberverse.asterion.ui.theme.PillShape

/** How chapters are laid out. Persisted, because it is a standing preference, not a per-visit one. */
enum class ChapterLayout { LIST, GRID }

/** Chapters per range chip. 100 keeps the rail short even for a 4,000-chapter novel (40 chips). */
const val CHAPTER_RANGE_SIZE = 100

/**
 * A numbered tile, three to a row.
 *
 * For a long series the chapter number is the only thing anyone navigates by - titles are usually
 * "Chapter 1042" restated, or a spoiler. Making the number the whole control means a screen shows
 * roughly thirty chapters instead of eight.
 */
@Composable
fun ChapterTile(
    chapter: Chapter,
    isRead: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chapterTileScale",
    )
    val container by animateColorAsState(
        targetValue = if (isRead) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chapterTileContainer",
    )

    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chapter.chapterNumber.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            // Read chapters recede rather than disappear - still legible, clearly already visited.
            color = if (isRead) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            textAlign = TextAlign.Center,
        )

        if (isDownloaded) {
            Icon(
                PhosphorIcons.DownloadDone,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(13.dp),
            )
        }
    }
}

/**
 * A chapter as a row. Rebuilt as a card with a number badge rather than a Material ListItem with a
 * chevron, so list mode belongs to the same design language as grid mode.
 */
@Composable
fun ChapterRow(
    chapter: Chapter,
    isRead: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chapterRowScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                chapter.chapterNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRead) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRead) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isDownloaded) {
            Icon(
                PhosphorIcons.DownloadDone,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Icon(
            PhosphorIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Range chips - "1-100", "101-200" - so a long novel is reachable in two taps rather than a drag.
 *
 * The anime episode browser already proved this pattern in-app; this is the same idea generalised
 * and given the new design language.
 */
@Composable
fun ChapterRangeRail(
    totalChapters: Int,
    selectedRange: Int,
    onRangeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalChapters <= CHAPTER_RANGE_SIZE) return
    val rangeCount = (totalChapters + CHAPTER_RANGE_SIZE - 1) / CHAPTER_RANGE_SIZE
    val haptics = LocalHapticFeedback.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items((0 until rangeCount).toList()) { index ->
            val start = index * CHAPTER_RANGE_SIZE + 1
            val end = minOf((index + 1) * CHAPTER_RANGE_SIZE, totalChapters)
            val selected = index == selectedRange
            Box(
                Modifier
                    .height(34.dp)
                    .clip(PillShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRangeSelected(index)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$start-$end",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** The list/grid switch, a two-segment control that mirrors the pill language used elsewhere. */
@Composable
fun ChapterLayoutToggle(
    layout: ChapterLayout,
    onLayoutChange: (ChapterLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ChapterLayout.entries.forEach { option ->
            val selected = option == layout
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "layoutToggleContainer",
            )
            Box(
                Modifier
                    .size(width = 40.dp, height = 30.dp)
                    .clip(PillShape)
                    .background(container)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLayoutChange(option)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (option == ChapterLayout.LIST) PhosphorIcons.List else PhosphorIcons.GridView,
                    contentDescription = if (option == ChapterLayout.LIST) "List view" else "Grid view",
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/**
 * Jump straight to a chapter number.
 *
 * With four thousand chapters, neither a drag nor forty range chips gets you to chapter 3,217
 * quickly. Someone resuming a long series usually knows exactly where they stopped.
 */
@Composable
fun JumpToChapterDialog(
    totalChapters: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val parsed = value.toIntOrNull()
    val isValid = parsed != null && parsed in 1..totalChapters

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to chapter", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { entered -> value = entered.filter { it.isDigit() }.take(6) },
                    placeholder = { Text("1 - $totalChapters") },
                    singleLine = true,
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { if (isValid) onJump(parsed) }),
                )
                if (value.isNotEmpty() && !isValid) {
                    Text(
                        "This novel has $totalChapters chapters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onJump(parsed) }, enabled = isValid) { Text("Go") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    )
}
