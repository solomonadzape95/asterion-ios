package cloud.cyberverse.asterion.ui.football

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.ui.components.ErrorState
import cloud.cyberverse.asterion.data.model.FootballTeam
import cloud.cyberverse.asterion.ui.components.AsterionAsyncImage
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.components.AsterionWordmark
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballCatalogScreen(onMatchClick: (FootballMatch) -> Unit, viewModel: FootballCatalogViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AsterionTopBar(title = { AsterionWordmark() }) }) { padding ->
        when (val current = state) {
            is FootballCatalogState.Loading -> AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))

            is FootballCatalogState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            is FootballCatalogState.Loaded -> {
                val groups = current.matches
                    .sortedBy { it.kickoffMillis }
                    .groupBy { startOfDay(it.kickoffMillis) }
                    .toSortedMap()

                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    groups.forEach { (day, matches) ->
                        item(key = "header-$day") {
                            Text(
                                dayLabel(day),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                            )
                        }
                        items(matches, key = { it.id }) { match ->
                            MatchRow(match = match, onClick = { onMatchClick(match) }, modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(millis)).uppercase(Locale.getDefault())

@Composable
private fun MatchRow(match: FootballMatch, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val kickoffFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
            Text(
                kickoffFormatter.format(Date(match.kickoffMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (match.isLive) {
                Text(
                    "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            TeamLine(match.teams?.home, match.homeFallback)
            TeamLine(match.teams?.away, match.awayFallback, topPadding = 8.dp)
        }

        if (match.popular) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = "Popular match",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp).size(18.dp),
            )
        }
    }
}

@Composable
private fun TeamLine(team: FootballTeam?, fallback: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = topPadding)) {
        if (team?.badgeUrl != null) {
            AsterionAsyncImage(
                model = team.badgeUrl,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            team?.name ?: fallback,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private val FootballMatch.homeFallback: String
    get() = title.split(" vs ").firstOrNull() ?: title

private val FootballMatch.awayFallback: String
    get() = title.split(" vs ").getOrNull(1) ?: "Opponent"
