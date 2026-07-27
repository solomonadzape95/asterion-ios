package cloud.cyberverse.asterion.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cloud.cyberverse.asterion.ui.anime.AnimeCatalogScreen
import cloud.cyberverse.asterion.ui.anime.AnimeDetailScreen
import cloud.cyberverse.asterion.ui.anime.AnimeEpisodePlayerScreen
import cloud.cyberverse.asterion.ui.components.AsterionBottomBar
import cloud.cyberverse.asterion.ui.components.AsterionTab
import cloud.cyberverse.asterion.ui.football.FootballCatalogScreen
import cloud.cyberverse.asterion.ui.football.FootballPlayerScreen
import cloud.cyberverse.asterion.ui.football.FootballStreamsScreen
import cloud.cyberverse.asterion.ui.home.HomeScreen
import cloud.cyberverse.asterion.ui.movies.MovieCatalogScreen
import cloud.cyberverse.asterion.ui.movies.MovieDetailScreen
import cloud.cyberverse.asterion.ui.movies.MoviePlayerScreen
import cloud.cyberverse.asterion.ui.novels.ChapterReaderScreen
import cloud.cyberverse.asterion.ui.novels.NovelDetailScreen
import cloud.cyberverse.asterion.ui.novels.NovelsListScreen
import cloud.cyberverse.asterion.ui.profile.ProfileScreen
import cloud.cyberverse.asterion.ui.downloads.DownloadsScreen
import java.net.URLDecoder
import java.net.URLEncoder

private const val ROUTE_PROFILE = "profile"
private const val ROUTE_DOWNLOADS = "downloads"
private const val ROUTE_HOME = "home"
private const val ROUTE_NOVELS = "novels"
private const val ROUTE_NOVEL_DETAIL = "novels/{novelId}"
private const val ROUTE_CHAPTER = "novels/{novelId}/chapters/{chapterNumber}"
private const val ROUTE_ANIME = "anime"
private const val ROUTE_ANIME_DETAIL = "anime/{slug}"
private const val ROUTE_ANIME_PLAYER = "anime/play/{animeId}/{episodeNumber}/{title}/{imageUrl}"
private const val ROUTE_MOVIES = "movies"
private const val ROUTE_MOVIE_DETAIL = "movies/{slug}"
private const val ROUTE_MOVIE_PLAYER = "movies/play/{slug}"
private const val ROUTE_FOOTBALL = "football"
private const val ROUTE_FOOTBALL_MATCH = "football/{matchId}"
private const val ROUTE_FOOTBALL_PLAYER = "football/play/{embedUrl}/{label}"

private val tabRoutes = AsterionTab.entries.map { it.route }.toSet()
private fun isTabEntry(entry: NavBackStackEntry) = entry.destination.route in tabRoutes

@Composable
fun AsterionNavHost() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentTab = AsterionTab.entries.firstOrNull { tab ->
        currentEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                AsterionBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            // A soft fade + rise/fall on push navigation into a detail/player screen -
            // AsterionMac's "coordinated app motion" applied to the nav transition. Switching
            // between bottom-nav tabs is excluded and gets a quick crossfade instead: it's a
            // lateral jump, not a "go deeper" action, and the slide made every tab tap feel slow.
            enterTransition = {
                if (isTabEntry(initialState) && isTabEntry(targetState)) {
                    fadeIn(tween(120))
                } else {
                    fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 }
                }
            },
            exitTransition = {
                if (isTabEntry(initialState) && isTabEntry(targetState)) fadeOut(tween(100)) else fadeOut(tween(150))
            },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 24 } },
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onAnimeClick = { title -> navController.navigate("anime/${title.slug}") },
                    onMovieClick = { title -> navController.navigate("movies/${title.slug}") },
                    onNovelClick = { novel -> navController.navigate("novels/${novel.id}") },
                    onFootballClick = { match -> navController.navigate("football/${match.id}") },
                )
            }
            composable(ROUTE_NOVELS) {
                NovelsListScreen(onNovelClick = { novel -> navController.navigate("novels/${novel.id}") })
            }
            composable(
                ROUTE_NOVEL_DETAIL,
                arguments = listOf(navArgument("novelId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val novelId = backStackEntry.arguments?.getString("novelId").orEmpty()
                NovelDetailScreen(
                    novelId = novelId,
                    onChapterClick = { chapter -> navController.navigate("novels/$novelId/chapters/${chapter.chapterNumber}") },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                ROUTE_CHAPTER,
                arguments = listOf(
                    navArgument("novelId") { type = NavType.StringType },
                    navArgument("chapterNumber") { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val novelId = backStackEntry.arguments?.getString("novelId").orEmpty()
                val chapterNumber = backStackEntry.arguments?.getInt("chapterNumber") ?: 0
                ChapterReaderScreen(
                    novelId = novelId,
                    chapterNumber = chapterNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChapter = { newNumber ->
                        navController.navigate("novels/$novelId/chapters/$newNumber") {
                            popUpTo(ROUTE_CHAPTER) { inclusive = true }
                        }
                    },
                )
            }
            composable(ROUTE_ANIME) {
                AnimeCatalogScreen(onTitleClick = { title -> navController.navigate("anime/${title.slug}") })
            }
            composable(
                ROUTE_ANIME_DETAIL,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug").orEmpty()
                AnimeDetailScreen(
                    slug = slug,
                    onEpisodeClick = { show, episode ->
                        val title = URLEncoder.encode(show.title, "UTF-8")
                        val imageUrl = URLEncoder.encode(show.imageUrl.orEmpty(), "UTF-8")
                        navController.navigate("anime/play/${show.id}/${episode.number}/$title/$imageUrl")
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                ROUTE_ANIME_PLAYER,
                arguments = listOf(
                    navArgument("animeId") { type = NavType.StringType },
                    navArgument("episodeNumber") { type = NavType.IntType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("imageUrl") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId").orEmpty()
                val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: 0
                val title = URLDecoder.decode(backStackEntry.arguments?.getString("title").orEmpty(), "UTF-8")
                val imageUrl = URLDecoder.decode(backStackEntry.arguments?.getString("imageUrl").orEmpty(), "UTF-8")
                AnimeEpisodePlayerScreen(
                    animeId = animeId,
                    episodeNumber = episodeNumber,
                    showTitle = title.ifBlank { "Anime" },
                    showImageUrl = imageUrl.ifBlank { null },
                )
            }
            composable(ROUTE_MOVIES) {
                MovieCatalogScreen(onTitleClick = { title -> navController.navigate("movies/${title.slug}") })
            }
            composable(
                ROUTE_MOVIE_DETAIL,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug").orEmpty()
                MovieDetailScreen(
                    slug = slug,
                    onPlayClick = { navController.navigate("movies/play/$slug") },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                ROUTE_MOVIE_PLAYER,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug").orEmpty()
                MoviePlayerScreen(slug = slug)
            }
            composable(ROUTE_FOOTBALL) {
                FootballCatalogScreen(onMatchClick = { match -> navController.navigate("football/${match.id}") })
            }
            composable(
                ROUTE_FOOTBALL_MATCH,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
                FootballStreamsScreen(
                    matchId = matchId,
                    onStreamClick = { stream ->
                        val encodedUrl = URLEncoder.encode(stream.embedUrl, "UTF-8")
                        val encodedLabel = URLEncoder.encode(stream.displayName, "UTF-8")
                        navController.navigate("football/play/$encodedUrl/$encodedLabel")
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                ROUTE_FOOTBALL_PLAYER,
                arguments = listOf(
                    navArgument("embedUrl") { type = NavType.StringType },
                    navArgument("label") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("embedUrl").orEmpty()
                val encodedLabel = backStackEntry.arguments?.getString("label").orEmpty()
                FootballPlayerScreen(
                    embedUrl = URLDecoder.decode(encodedUrl, "UTF-8"),
                    streamLabel = URLDecoder.decode(encodedLabel, "UTF-8"),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_PROFILE) {
                ProfileScreen(onDownloadsClick = { navController.navigate(ROUTE_DOWNLOADS) })
            }
            composable(ROUTE_DOWNLOADS) {
                DownloadsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenMovie = { slug -> navController.navigate("movies/play/$slug") },
                    onOpenAnimeEpisode = { animeId, episode, title, imageUrl ->
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        val encodedImageUrl = URLEncoder.encode(imageUrl.orEmpty(), "UTF-8")
                        navController.navigate("anime/play/$animeId/$episode/$encodedTitle/$encodedImageUrl")
                    },
                    onOpenNovelChapter = { novelId, chapter -> navController.navigate("novels/$novelId/chapters/$chapter") },
                )
            }
        }
    }
}
