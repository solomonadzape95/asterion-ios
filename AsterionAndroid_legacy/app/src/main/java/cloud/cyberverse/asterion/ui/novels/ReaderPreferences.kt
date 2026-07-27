package cloud.cyberverse.asterion.ui.novels

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore by preferencesDataStore(name = "reader_prefs")

enum class ReaderTheme { PAPER, SEPIA, INK }
enum class ReaderFont { SERIF, SANS, MONOSPACE }

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val font: ReaderFont = ReaderFont.SERIF,
    val fontSizeSp: Float = 17f,
)

data class ReaderPalette(val background: Color, val text: Color, val muted: Color, val border: Color)

fun ReaderTheme.palette(): ReaderPalette = when (this) {
    // Matches AsterionMac's ReaderTheme trio (paper/sepia/ink) so the same mental model carries over.
    ReaderTheme.PAPER -> ReaderPalette(Color(0xFFF7F1E3), Color(0xFF2B2620), Color(0xFF6B6560), Color(0xFFDDD7CE))
    ReaderTheme.SEPIA -> ReaderPalette(Color(0xFFE9DCC3), Color(0xFF3A2E1F), Color(0xFF7A6A4F), Color(0xFFD2C0A0))
    ReaderTheme.INK -> ReaderPalette(Color(0xFF121114), Color(0xFFF0EEEA), Color(0xFFA8A29E), Color(0xFF38343A))
}

fun ReaderFont.fontFamily(): FontFamily = when (this) {
    ReaderFont.SERIF -> FontFamily.Serif
    ReaderFont.SANS -> FontFamily.SansSerif
    ReaderFont.MONOSPACE -> FontFamily.Monospace
}

fun ReaderFont.displayName(): String = when (this) {
    ReaderFont.SERIF -> "Serif"
    ReaderFont.SANS -> "Sans"
    ReaderFont.MONOSPACE -> "Mono"
}

class ReaderPreferences(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("reader_theme")
        val FONT = stringPreferencesKey("reader_font")
        val FONT_SIZE = floatPreferencesKey("reader_font_size")
    }

    val settings: Flow<ReaderSettings> = context.readerDataStore.data.map { prefs ->
        ReaderSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.PAPER,
            font = prefs[Keys.FONT]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() } ?: ReaderFont.SERIF,
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 17f,
        )
    }

    suspend fun setTheme(theme: ReaderTheme) {
        context.readerDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setFont(font: ReaderFont) {
        context.readerDataStore.edit { it[Keys.FONT] = font.name }
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.readerDataStore.edit { it[Keys.FONT_SIZE] = sizeSp.coerceIn(14f, 30f) }
    }
}
