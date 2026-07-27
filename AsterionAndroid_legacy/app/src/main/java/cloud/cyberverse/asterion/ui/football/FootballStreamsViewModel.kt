package cloud.cyberverse.asterion.ui.football

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.data.model.FootballStream
import cloud.cyberverse.asterion.data.model.FootballStreamRequest
import cloud.cyberverse.asterion.data.remote.FootballApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FootballStreamsState {
    data object Loading : FootballStreamsState
    data class Loaded(val match: FootballMatch, val streams: List<FootballStream>) : FootballStreamsState
    data class Error(val message: String) : FootballStreamsState
}

// The match itself isn't refetchable by id, so this looks it up from the same feeds the catalog
// already shows, then resolves its streams.
class FootballStreamsViewModel(private val api: FootballApiService, private val matchId: String) : ViewModel() {
    private val _state = MutableStateFlow<FootballStreamsState>(FootballStreamsState.Loading)
    val state: StateFlow<FootballStreamsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val match = findMatch()
                if (match == null) {
                    FootballStreamsState.Error("Match not found.")
                } else {
                    val request = FootballStreamRequest(
                        matchId = match.id,
                        sources = match.sources,
                        homeTeam = match.teams?.home?.name,
                        awayTeam = match.teams?.away?.name,
                    )
                    FootballStreamsState.Loaded(match, api.streams(request).data.streams)
                }
            } catch (error: Exception) {
                FootballStreamsState.Error(error.message ?: "Unknown error")
            }
        }
    }

    /**
     * The upstream provider resolves the schedule, live, and popular feeds independently, and the
     * same match can carry a different (sometimes larger) `sources` list depending which feed
     * surfaced it - AsterionMac's dedicated Football tab defaults to the live feed while Android's
     * only ever queried the general schedule, so a match with richer sources on the live/popular
     * feed showed fewer streams here than on Mac. Union every feed's source list for this match
     * (deduped by provider+id) so we always resolve the maximum available set of streams,
     * regardless of which feed happens to answer first.
     */
    private suspend fun findMatch(): FootballMatch? = coroutineScope {
        val scheduleDeferred = async { runCatching { api.schedule().data }.getOrDefault(emptyList()) }
        val liveDeferred = async { runCatching { api.liveMatches().data }.getOrDefault(emptyList()) }
        val popularDeferred = async { runCatching { api.popularMatches().data }.getOrDefault(emptyList()) }

        val candidates = (scheduleDeferred.await() + liveDeferred.await() + popularDeferred.await())
            .filter { it.id == matchId }
        val base = candidates.firstOrNull() ?: return@coroutineScope null

        val mergedSources = candidates
            .flatMap { it.sources }
            .distinctBy { it.source to it.id }
        base.copy(sources = mergedSources.ifEmpty { base.sources })
    }
}
