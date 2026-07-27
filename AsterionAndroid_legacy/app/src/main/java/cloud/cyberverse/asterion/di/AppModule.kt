package cloud.cyberverse.asterion.di

import cloud.cyberverse.asterion.BuildConfig
import cloud.cyberverse.asterion.data.download.NovelDownloadRepository
import cloud.cyberverse.asterion.data.download.VideoDownloadManager
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.data.remote.AnimeApiService
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.data.remote.ClerkAuthInterceptor
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import cloud.cyberverse.asterion.ui.anime.AnimeCatalogViewModel
import cloud.cyberverse.asterion.ui.anime.AnimeDetailViewModel
import cloud.cyberverse.asterion.ui.anime.AnimeEpisodePlayerViewModel
import cloud.cyberverse.asterion.data.remote.FootballApiService
import cloud.cyberverse.asterion.data.remote.MovieApiService
import cloud.cyberverse.asterion.ui.downloads.DownloadsViewModel
import cloud.cyberverse.asterion.ui.football.FootballCatalogViewModel
import cloud.cyberverse.asterion.ui.football.FootballStreamsViewModel
import cloud.cyberverse.asterion.ui.home.HomeViewModel
import cloud.cyberverse.asterion.ui.movies.MovieCatalogViewModel
import cloud.cyberverse.asterion.ui.movies.MovieDetailViewModel
import cloud.cyberverse.asterion.ui.movies.MoviePlayerViewModel
import cloud.cyberverse.asterion.ui.novels.ChapterReaderViewModel
import cloud.cyberverse.asterion.ui.novels.NovelDetailViewModel
import cloud.cyberverse.asterion.ui.novels.NovelsListViewModel
import cloud.cyberverse.asterion.ui.novels.ReaderPreferences
import cloud.cyberverse.asterion.ui.settings.AppSettingsPreferences
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true }

// Anime and movies are each served by their own separate scraper service, not the main Asterion API.
private const val ANIME_BASE_URL = "https://asterion-scraper.cyberverse.cloud/api/amp/"
private const val MOVIES_BASE_URL = "https://asterion-movies.cyberverse.cloud/api/"
private const val FOOTBALL_BASE_URL = "https://asterion-football.cyberverse.cloud/api/"

val networkModule = module {
    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    single {
        // The anime stream endpoint scrapes a third-party site live and can take well over 10s;
        // AsterionMac relies on URLSession's 60s default for the same service, matched here.
        OkHttpClient.Builder()
            // Only attaches auth to the main Asterion API (host-checked inside the interceptor) -
            // the anime/movie/football scraper services share this same client and stay
            // unauthenticated.
            .addInterceptor(ClerkAuthInterceptor(URI(BuildConfig.API_BASE_URL).host))
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl("${BuildConfig.API_BASE_URL}/")
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AsterionApiService::class.java)
    }
    single {
        Retrofit.Builder()
            .baseUrl(ANIME_BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnimeApiService::class.java)
    }
    single {
        Retrofit.Builder()
            .baseUrl(MOVIES_BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MovieApiService::class.java)
    }
    single {
        Retrofit.Builder()
            .baseUrl(FOOTBALL_BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FootballApiService::class.java)
    }
    single { ReaderPreferences(androidContext()) }
    single { AppSettingsPreferences(androidContext()) }
    single { MediaAccountRepository(get()) }
}

val downloadModule = module {
    single { DownloadIndexStore(androidContext()) }
    single { VideoDownloadManager(androidContext(), get(), get()) }
    single { NovelDownloadRepository(androidContext()) }
    viewModel { DownloadsViewModel(get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { NovelsListViewModel(get()) }
    viewModel { (novelId: String) -> NovelDetailViewModel(get(), novelId) }
    viewModel { (novelId: String, chapterNumber: Int) -> ChapterReaderViewModel(get(), novelId, chapterNumber, androidContext()) }
    viewModel { AnimeCatalogViewModel(get()) }
    viewModel { (slug: String) -> AnimeDetailViewModel(get(), get(), slug) }
    viewModel { (animeId: String, episodeNumber: Int, showTitle: String, showImageUrl: String?) ->
        AnimeEpisodePlayerViewModel(get(), get(), animeId, episodeNumber, showTitle, showImageUrl, get(), get())
    }
    viewModel { MovieCatalogViewModel(get()) }
    viewModel { (slug: String) -> MovieDetailViewModel(get(), get(), slug) }
    viewModel { (slug: String) -> MoviePlayerViewModel(get(), get(), slug, get(), get()) }
    viewModel { FootballCatalogViewModel(get()) }
    viewModel { (matchId: String) -> FootballStreamsViewModel(get(), matchId) }
}
