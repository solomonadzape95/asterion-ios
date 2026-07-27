package cloud.cyberverse.asterion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Novel(
    @SerialName("_id") val id: String,
    val title: String,
    val author: String? = null,
    val rank: String? = null,
    val totalChapters: String? = null,
    val views: String? = null,
    val bookmarks: String? = null,
    val status: String? = null,
    val genres: List<String>? = null,
    val summary: String? = null,
    val imageUrl: String? = null,
    val rating: Double? = null,
)

@Serializable
data class Chapter(
    @SerialName("_id") val id: String,
    val chapterNumber: Int,
    val title: String,
    val content: String? = null,
    val url: String? = null,
)

@Serializable
data class ListMeta(
    val count: Int = 0,
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
)

@Serializable
data class ListEnvelope<T>(val data: List<T>, val meta: ListMeta? = null)

@Serializable
data class ItemEnvelope<T>(val data: T)
