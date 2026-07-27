package cloud.cyberverse.asterion.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.local.DownloadEntry
import cloud.cyberverse.asterion.data.local.DownloadPhase
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit = {},
    onOpenMovie: (String) -> Unit = {},
    onOpenAnimeEpisode: (String, Int, String, String?) -> Unit = { _, _, _, _ -> },
    onOpenNovelChapter: (String, Int) -> Unit = { _, _ -> },
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        topBar = { AsterionTopBar(title = "Downloads", onBack = onNavigateBack) },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(entries, key = { it.id }) { entry ->
                DownloadRow(
                    entry = entry,
                    onClick = {
                        if (entry.phase != DownloadPhase.COMPLETED) return@DownloadRow
                        when (entry.contentType) {
                            DownloadContentType.MOVIE -> onOpenMovie(entry.contentId)
                            DownloadContentType.ANIME -> onOpenAnimeEpisode(
                                entry.contentId,
                                entry.unitId?.toIntOrNull() ?: 0,
                                entry.contentTitle,
                                entry.imageUrl,
                            )
                            DownloadContentType.NOVEL -> onOpenNovelChapter(entry.contentId, entry.unitId?.toIntOrNull() ?: 0)
                        }
                    },
                    onDelete = { viewModel.remove(entry) },
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(entry: DownloadEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.contentTitle) },
        supportingContent = {
            Column {
                entry.unitTitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (entry.phase == DownloadPhase.DOWNLOADING || entry.phase == DownloadPhase.QUEUED) {
                    LinearProgressIndicator(
                        progress = { entry.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                } else if (entry.phase == DownloadPhase.FAILED) {
                    Text(
                        entry.errorMessage ?: "Download failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        leadingContent = {
            if (entry.imageUrl != null) {
                AsterionAsyncImage(
                    model = entry.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (entry.phase) {
                    DownloadPhase.DOWNLOADING, DownloadPhase.QUEUED -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    DownloadPhase.COMPLETED -> Icon(Icons.Filled.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                    DownloadPhase.FAILED -> Icon(Icons.Filled.Error, contentDescription = "Failed", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove download")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
