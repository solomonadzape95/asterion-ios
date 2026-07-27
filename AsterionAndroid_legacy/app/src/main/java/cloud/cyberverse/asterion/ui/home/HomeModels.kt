package cloud.cyberverse.asterion.ui.home

import cloud.cyberverse.asterion.data.model.AnimeTitle
import cloud.cyberverse.asterion.data.model.MovieTitle
import cloud.cyberverse.asterion.data.model.Novel

/** A single card surfaced on the Home dashboard, regardless of which catalog it came from. */
sealed interface HomeCatalogItem {
    val imageUrl: String?
    val title: String
    val subtitle: String
    val badge: String

    data class AnimeItem(val anime: AnimeTitle) : HomeCatalogItem {
        override val imageUrl get() = anime.imageUrl
        override val title get() = anime.title
        override val subtitle get() = anime.episodeLabel ?: anime.type.orEmpty()
        override val badge get() = "Anime"
    }

    data class MovieItem(val movie: MovieTitle) : HomeCatalogItem {
        override val imageUrl get() = movie.imageUrl
        override val title get() = movie.title
        override val subtitle get() = listOfNotNull(movie.year, movie.runtime).joinToString(" · ")
        override val badge get() = if (movie.type?.contains("tv", ignoreCase = true) == true) "TV Series" else "Movie"
    }

    data class NovelItem(val novel: Novel) : HomeCatalogItem {
        override val imageUrl get() = novel.imageUrl
        override val title get() = novel.title
        override val subtitle get() = novel.author.orEmpty()
        override val badge get() = "Novel"
    }
}

/**
 * Round-robins several lists into one, so a shelf mixing anime/movies/novels doesn't read as
 * "all anime, then all movies, then all novels" - mirrors AsterionMac's HomeDashboardView.interleaved.
 */
fun <T> interleave(vararg lists: List<T>): List<T> {
    val maxSize = lists.maxOfOrNull { it.size } ?: 0
    return (0 until maxSize).flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }
}
