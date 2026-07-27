package cloud.cyberverse.asterion.ui.football

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.EmbedWebView

@Composable
fun FootballPlayerScreen(embedUrl: String, streamLabel: String? = null, onNavigateBack: () -> Unit = {}) {
    var isLoading by remember(embedUrl) { mutableStateOf(true) }
    var failed by remember(embedUrl) { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        AsterionTopBar(title = streamLabel, onBack = onNavigateBack)

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (!failed) {
                EmbedWebView(
                    url = embedUrl,
                    reloadKey = embedUrl to reloadKey,
                    onPageFinished = { isLoading = false },
                    onMainFrameError = {
                        isLoading = false
                        failed = true
                    },
                )
                if (isLoading) AsterionLoadingBox()
            } else {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "This stream failed to load.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 80.dp),
                    )
                    Text(
                        "Go back and try a different stream, or retry this one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                    )
                    Button(onClick = {
                        failed = false
                        isLoading = true
                        reloadKey++
                    }) { Text("Retry") }
                }
            }
        }
    }
}
