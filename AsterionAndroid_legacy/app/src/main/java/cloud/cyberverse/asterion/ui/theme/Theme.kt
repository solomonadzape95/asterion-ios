package cloud.cyberverse.asterion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cloud.cyberverse.asterion.ui.settings.AppSettings
import cloud.cyberverse.asterion.ui.settings.AppSettingsPreferences
import cloud.cyberverse.asterion.ui.settings.AppThemeMode
import org.koin.compose.koinInject

private val LightColors = lightColorScheme(
    primary = AsterionCrimson,
    onPrimary = AsterionLightSurface,
    primaryContainer = AsterionLightAccentContainer,
    onPrimaryContainer = AsterionLightOnAccentContainer,
    secondary = AsterionCrimson,
    onSecondary = AsterionLightSurface,
    secondaryContainer = AsterionLightAccentContainer,
    onSecondaryContainer = AsterionLightOnAccentContainer,
    tertiary = AsterionCrimson,
    onTertiary = AsterionLightSurface,
    tertiaryContainer = AsterionLightAccentContainer,
    onTertiaryContainer = AsterionLightOnAccentContainer,
    background = AsterionLightBackground,
    onBackground = AsterionLightOnBackground,
    surface = AsterionLightSurface,
    onSurface = AsterionLightOnBackground,
    surfaceVariant = AsterionLightCard,
    onSurfaceVariant = AsterionLightMuted,
    outline = AsterionLightBorder,
)

private val DarkColors = darkColorScheme(
    primary = AsterionCrimsonDark,
    onPrimary = AsterionDarkBackground,
    primaryContainer = AsterionDarkAccentContainer,
    onPrimaryContainer = AsterionDarkOnAccentContainer,
    secondary = AsterionCrimsonDark,
    onSecondary = AsterionDarkBackground,
    secondaryContainer = AsterionDarkAccentContainer,
    onSecondaryContainer = AsterionDarkOnAccentContainer,
    tertiary = AsterionCrimsonDark,
    onTertiary = AsterionDarkBackground,
    tertiaryContainer = AsterionDarkAccentContainer,
    onTertiaryContainer = AsterionDarkOnAccentContainer,
    background = AsterionDarkBackground,
    onBackground = AsterionDarkOnBackground,
    surface = AsterionDarkSurface,
    onSurface = AsterionDarkOnBackground,
    surfaceVariant = AsterionDarkCard,
    onSurfaceVariant = AsterionDarkMuted,
    outline = AsterionDarkBorder,
)

@Composable
fun AsterionTheme(
    settingsPreferences: AppSettingsPreferences = koinInject(),
    content: @Composable () -> Unit,
) {
    val settings by settingsPreferences.settings.collectAsState(initial = AppSettings())
    val useDarkTheme = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colors = if (useDarkTheme) DarkColors else LightColors
    val typography = remember(settings.font) { asterionTypography(settings.font.headingFontFamily()) }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = AsterionShapes, content = content)
}
