package cloud.cyberverse.asterion.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cloud.cyberverse.asterion.ui.novels.ChapterLayout
import cloud.cyberverse.asterion.ui.theme.AccentColor
import cloud.cyberverse.asterion.ui.novels.ReaderFont
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    // Reuses ReaderFont (already the reading page's font choices) as the app-wide heading/display
    // font too, rather than inventing a second, parallel font enum.
    val font: ReaderFont = ReaderFont.SERIF,
    // How chapters are laid out on a novel. A standing preference: someone who reads long series
    // wants the numbered grid every time, not to re-pick it on every novel they open.
    val chapterLayout: ChapterLayout = ChapterLayout.LIST,
    val accent: AccentColor = AccentColor.Gold,
)

/** Persisted, device-local app preferences (theme, font) - separate from [ReaderPreferences],
 * which only covers the in-chapter reading experience. */
class AppSettingsPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val FONT = stringPreferencesKey("app_font")
        val CHAPTER_LAYOUT = stringPreferencesKey("chapter_layout")
        val ACCENT = stringPreferencesKey("accent_color")
    }

    val settings: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            font = prefs[Keys.FONT]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() } ?: ReaderFont.SERIF,
            chapterLayout = prefs[Keys.CHAPTER_LAYOUT]
                ?.let { runCatching { ChapterLayout.valueOf(it) }.getOrNull() } ?: ChapterLayout.LIST,
            accent = prefs[Keys.ACCENT]
                ?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.Gold,
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appSettingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFont(font: ReaderFont) {
        context.appSettingsDataStore.edit { it[Keys.FONT] = font.name }
    }

    suspend fun setChapterLayout(layout: ChapterLayout) {
        context.appSettingsDataStore.edit { it[Keys.CHAPTER_LAYOUT] = layout.name }
    }

    suspend fun setAccent(accent: AccentColor) {
        context.appSettingsDataStore.edit { it[Keys.ACCENT] = accent.name }
    }
}
