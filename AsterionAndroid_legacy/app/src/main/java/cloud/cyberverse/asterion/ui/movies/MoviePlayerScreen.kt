package cloud.cyberverse.asterion.ui.movies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.EmbedWebView
import cloud.cyberverse.asterion.ui.components.PlaybackSource
import cloud.cyberverse.asterion.ui.components.PlaybackSourceKind
import cloud.cyberverse.asterion.ui.components.VideoPlayerScaffold
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MoviePlayerScreen(slug: String, viewModel: MoviePlayerViewModel = koinViewModel(parameters = { parametersOf(slug) })) {
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is MoviePlayerState.Loading -> AsterionLoadingIndicator()
            is MoviePlayerState.Error -> Text("Couldn't play this title: ${current.message}")
            is MoviePlayerState.Ready -> {
                val selectedSource = current.sources.getOrNull(current.selectedIndex)
                if (selectedSource?.kind == PlaybackSourceKind.WEB) {
                    // No direct URL exists for this source (it's an embed page) - ExoPlayer can't
                    // play it at all, so it renders in a WebView instead, with its own compact
                    // source switcher since VideoPlayerControls (built for the ExoPlayer surface)
                    // doesn't apply here.
                    WebSourcePlayer(
                        url = selectedSource.uri,
                        sources = current.sources,
                        selectedIndex = current.selectedIndex,
                        onSelectSource = viewModel::selectSource,
                    )
                } else {
                    VideoPlayerScaffold(
                        player = current.player,
                        sources = current.sources,
                        selectedSourceIndex = current.selectedIndex,
                        onSelectSource = viewModel::selectSource,
                        playbackError = current.playbackError,
                        onRetry = viewModel::retryCurrentSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun WebSourcePlayer(
    url: String,
    sources: List<PlaybackSource>,
    selectedIndex: Int,
    onSelectSource: (Int) -> Unit,
) {
    var showSourceMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        EmbedWebView(url = url, reloadKey = url, modifier = Modifier.fillMaxSize())

        if (sources.size > 1) {
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                IconButton(onClick = { showSourceMenu = true }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Source", tint = Color.White)
                }
                DropdownMenu(expanded = showSourceMenu, onDismissRequest = { showSourceMenu = false }) {
                    sources.forEachIndexed { index, source ->
                        DropdownMenuItem(
                            text = { Text(source.label) },
                            leadingIcon = { if (index == selectedIndex) Icon(Icons.Filled.Check, contentDescription = null) },
                            onClick = {
                                showSourceMenu = false
                                onSelectSource(index)
                            },
                        )
                    }
                }
            }
        }
    }
}
