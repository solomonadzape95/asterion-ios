package cloud.cyberverse.asterion.ui.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import cloud.cyberverse.asterion.data.model.FootballStream
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.SectionHeader
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballStreamsScreen(
    matchId: String,
    onStreamClick: (FootballStream) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: FootballStreamsViewModel = koinViewModel(parameters = { parametersOf(matchId) }),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AsterionTopBar(onBack = onNavigateBack) }) { padding ->
        when (val current = state) {
            is FootballStreamsState.Loading -> AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))

            is FootballStreamsState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            is FootballStreamsState.Loaded -> LazyColumn(Modifier.padding(padding)) {
                item {
                    Column(Modifier.padding(20.dp)) {
                        AsterionAsyncImage(
                            model = current.match.posterUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Text(
                            current.match.displayTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                item {
                    SectionHeader(
                        title = "Streams",
                        subtitle = "Pick a language or quality.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                items(current.streams, key = { "${it.source}-${it.id}-${it.streamNumber}" }) { stream ->
                    ListItem(
                        headlineContent = { Text(stream.displayName) },
                        leadingContent = if (stream.hd) {
                            { Icon(Icons.Filled.Hd, contentDescription = "HD") }
                        } else {
                            null
                        },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onStreamClick(stream) },
                    )
                }
            }
        }
    }
}
