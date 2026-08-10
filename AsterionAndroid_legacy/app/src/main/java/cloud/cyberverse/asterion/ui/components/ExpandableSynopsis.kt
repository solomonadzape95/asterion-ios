package cloud.cyberverse.asterion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val trailingScrapeDateRegex = Regex("\\s+on [A-Z][a-z]+ \\d{1,2}, \\d{4}.*$")
private val missingSentenceSpaceRegex = Regex("([.!?])([A-Z])")
private const val PREVIEW_LENGTH = 280

/**
 * Scraped novel/show summaries sometimes have a literal "Show More" button label baked into the
 * text itself (dead text, since there's no real button behind it) plus a trailing scrape-date
 * suffix - this strips both, then offers a real Read more/less toggle. Mirrors AsterionMac's
 * `cleanSummary`/`synopsisPreview` in NovelDetailView.swift.
 */
private fun cleanSynopsis(raw: String): String {
    var cleaned = raw.replace("Show More", "")
    cleaned = trailingScrapeDateRegex.replace(cleaned, "")
    cleaned = missingSentenceSpaceRegex.replace(cleaned, "$1 $2")
    return cleaned.trim()
}

private fun synopsisPreview(clean: String, maxLength: Int): String {
    if (clean.length <= maxLength) return clean
    val prefix = clean.take(maxLength)
    val sentenceEnd = prefix.indexOfLast { it == '.' || it == '!' || it == '?' }
    if (sentenceEnd >= 0) return prefix.substring(0, sentenceEnd + 1)
    val wordBoundary = prefix.indexOfLast { it.isWhitespace() }
    if (wordBoundary >= 0) return prefix.substring(0, wordBoundary) + "…"
    return prefix + "…"
}

@Composable
fun ExpandableSynopsis(rawSummary: String?, modifier: Modifier = Modifier) {
    val cleanSummary = remember(rawSummary) { rawSummary?.let(::cleanSynopsis).orEmpty() }
    if (cleanSummary.isBlank()) return

    var expanded by rememberSaveable(rawSummary) { mutableStateOf(false) }
    val preview = remember(cleanSummary) { synopsisPreview(cleanSummary, PREVIEW_LENGTH) }
    val canToggle = preview != cleanSummary

    Column(modifier) {
        Text(
            text = if (expanded) cleanSummary else preview,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (canToggle) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expanded) "Show less" else "Read full synopsis",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        if (expanded) PhosphorIcons.KeyboardArrowUp else PhosphorIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
