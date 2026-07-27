package cloud.cyberverse.asterion.ui.anime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.VideoPlayerScaffold
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AnimeEpisodePlayerScreen(
    animeId: String,
    episodeNumber: Int,
    showTitle: String,
    showImageUrl: String?,
    viewModel: AnimeEpisodePlayerViewModel = koinViewModel(
        parameters = { parametersOf(animeId, episodeNumber, showTitle, showImageUrl) },
    ),
) {
    val state by viewModel.state.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is AnimeEpisodePlayerState.Loading -> AsterionLoadingIndicator()
            is AnimeEpisodePlayerState.Error -> Text("Couldn't play this episode: ${current.message}")
            is AnimeEpisodePlayerState.Ready -> VideoPlayerScaffold(
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
