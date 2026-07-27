package cloud.cyberverse.asterion.data.remote

import cloud.cyberverse.asterion.data.model.Envelope
import cloud.cyberverse.asterion.data.model.FootballMatch
import cloud.cyberverse.asterion.data.model.FootballStreamCollection
import cloud.cyberverse.asterion.data.model.FootballStreamRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FootballApiService {
    @GET("matches")
    suspend fun schedule(): Envelope<List<FootballMatch>>

    /** Same match can carry a different (sometimes richer) `sources` list here than in [schedule] -
     * the upstream provider resolves live matches separately from the general schedule feed. */
    @GET("matches/live")
    suspend fun liveMatches(): Envelope<List<FootballMatch>>

    /** Ditto for the curated "popular" feed - see [liveMatches]. */
    @GET("matches/popular")
    suspend fun popularMatches(): Envelope<List<FootballMatch>>

    @POST("streams")
    suspend fun streams(@Body request: FootballStreamRequest): Envelope<FootballStreamCollection>
}
