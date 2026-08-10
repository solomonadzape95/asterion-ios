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

private fun lightColors(accent: AccentColor) = lightColorScheme(
    primary = accent.light,
    onPrimary = AsterionLightSurface,
    primaryContainer = accent.lightContainer,
    onPrimaryContainer = accent.onLightContainer,
    secondary = accent.light,
    onSecondary = AsterionLightSurface,
    secondaryContainer = accent.lightContainer,
    onSecondaryContainer = accent.onLightContainer,
    tertiary = accent.light,
    onTertiary = AsterionLightSurface,
    tertiaryContainer = accent.lightContainer,
    onTertiaryContainer = accent.onLightContainer,
    background = AsterionLightBackground,
    onBackground = AsterionLightOnBackground,
    surface = AsterionLightSurface,
    onSurface = AsterionLightOnBackground,
    surfaceVariant = AsterionLightCard,
    onSurfaceVariant = AsterionLightMuted,
    outline = AsterionLightBorder,
)

private fun darkColors(accent: AccentColor) = darkColorScheme(
    primary = accent.dark,
    onPrimary = AsterionDarkBackground,
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.onDarkContainer,
    secondary = accent.dark,
    onSecondary = AsterionDarkBackground,
    secondaryContainer = accent.darkContainer,
    onSecondaryContainer = accent.onDarkContainer,
    tertiary = accent.dark,
    onTertiary = AsterionDarkBackground,
    tertiaryContainer = accent.darkContainer,
    onTertiaryContainer = accent.onDarkContainer,
    background = AsterionDarkBackground,
    onBackground = AsterionDarkOnBackground,
    surface = AsterionDarkSurface,
    onSurface = AsterionDarkOnBackground,
    surfaceVariant = AsterionDarkCard,
    onSurfaceVariant = AsterionDarkMuted,
    // The fourth value step, for controls resting on a card. Material has no better-named role
    // for this, and surfaceContainerHighest is what M3 components reach for by default.
    surfaceContainerHighest = AsterionDarkElevated,
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
    val colors = remember(useDarkTheme, settings.accent) {
        if (useDarkTheme) darkColors(settings.accent) else lightColors(settings.accent)
    }
    val typography = remember(settings.font) { asterionTypography(settings.font.headingFontFamily()) }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = AsterionShapes, content = content)
}
