package cloud.cyberverse.asterion.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.cyberverse.asterion.BuildConfig
import cloud.cyberverse.asterion.data.local.DownloadContentType
import cloud.cyberverse.asterion.data.local.DownloadIndexStore
import cloud.cyberverse.asterion.data.model.MediaAccountStats
import cloud.cyberverse.asterion.data.remote.AsterionApiService
import cloud.cyberverse.asterion.data.sync.MediaAccountRepository
import cloud.cyberverse.asterion.ui.components.AsterionLoadingBox
import cloud.cyberverse.asterion.ui.components.AsterionLoadingIndicator
import cloud.cyberverse.asterion.ui.components.AsterionTopBar
import cloud.cyberverse.asterion.ui.novels.ReaderFont
import cloud.cyberverse.asterion.ui.novels.ReaderPreferences
import cloud.cyberverse.asterion.ui.novels.ReaderSettings
import cloud.cyberverse.asterion.ui.novels.ReaderSettingsSheet
import cloud.cyberverse.asterion.ui.novels.displayName
import cloud.cyberverse.asterion.ui.settings.AppSettings
import cloud.cyberverse.asterion.ui.settings.AppSettingsPreferences
import cloud.cyberverse.asterion.ui.settings.AppThemeMode
import coil3.compose.AsyncImage
import com.clerk.api.Clerk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onDownloadsClick: () -> Unit = {},
    readerPreferences: ReaderPreferences = koinInject(),
    downloadIndexStore: DownloadIndexStore = koinInject(),
    appSettingsPreferences: AppSettingsPreferences = koinInject(),
    mediaAccountRepository: MediaAccountRepository = koinInject(),
    api: AsterionApiService = koinInject(),
) {
    val user by Clerk.userFlow.collectAsState()
    val readerSettings by readerPreferences.settings.collectAsState(initial = ReaderSettings())
    val appSettings by appSettingsPreferences.settings.collectAsState(initial = AppSettings())
    val downloads by downloadIndexStore.entries.collectAsState()
    val mediaSnapshot by mediaAccountRepository.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    var showReaderSettings by remember { mutableStateOf(false) }

    var savedNovelCount by remember { mutableStateOf<Int?>(null) }
    var isSyncing by remember { mutableStateOf(true) }
    var syncFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isSyncing = true
        val snapshot = mediaAccountRepository.refresh()
        val library = runCatching { api.library().data }.getOrNull()
        savedNovelCount = library?.size
        syncFailed = snapshot == null && library == null
        isSyncing = false
    }

    Scaffold(topBar = { AsterionTopBar(title = "Profile") }) { padding ->
        val currentUser = user
        if (currentUser == null) {
            AsterionLoadingBox(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        val initials = listOfNotNull(currentUser.firstName?.firstOrNull(), currentUser.lastName?.firstOrNull())
            .joinToString("")
            .ifEmpty { currentUser.emailAddresses?.firstOrNull()?.emailAddress?.firstOrNull()?.toString() ?: "?" }
        val displayName = listOfNotNull(currentUser.firstName, currentUser.lastName)
            .joinToString(" ")
            .ifBlank { currentUser.username ?: currentUser.emailAddresses?.firstOrNull()?.emailAddress ?: "Asterion reader" }
        val email = currentUser.emailAddresses?.firstOrNull()?.emailAddress
        val memberSinceYear = currentUser.createdAt?.let { SimpleDateFormat("yyyy", Locale.US).format(Date(it)) }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            ProfileHeader(
                imageUrl = currentUser.imageUrl,
                initials = initials,
                displayName = displayName,
                email = email,
                memberSinceYear = memberSinceYear,
                modifier = Modifier.padding(top = 20.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                StatCard(
                    icon = Icons.Filled.AutoStories,
                    value = downloads.count { it.contentType == DownloadContentType.NOVEL },
                    label = "Chapters",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Filled.LiveTv,
                    value = downloads.count { it.contentType == DownloadContentType.ANIME },
                    label = "Episodes",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Filled.Movie,
                    value = downloads.count { it.contentType == DownloadContentType.MOVIE },
                    label = "Movies",
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Library", modifier = Modifier.padding(top = 30.dp, bottom = 10.dp))
            ProfileCardRow(
                icon = Icons.Filled.Download,
                title = "Downloads",
                subtitle = if (downloads.isEmpty()) "Nothing downloaded yet" else "${downloads.size} item${if (downloads.size == 1) "" else "s"} saved offline",
                onClick = onDownloadsClick,
            )

            SectionLabel("Appearance", modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            AppearanceCard(
                settings = appSettings,
                onThemeSelected = { mode -> scope.launch { appSettingsPreferences.setThemeMode(mode) } },
                onFontSelected = { font -> scope.launch { appSettingsPreferences.setFont(font) } },
            )
            Spacer(Modifier.padding(top = 10.dp))
            ProfileCardRow(
                icon = Icons.Filled.TextFields,
                title = "Reading Settings",
                subtitle = "Theme, font, and text size for the reader",
                onClick = { showReaderSettings = true },
            )

            SectionLabel("Synced to Your Account", modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            SyncSection(
                isLoading = isSyncing,
                failed = syncFailed,
                savedNovels = savedNovelCount,
                stats = mediaSnapshot?.stats,
            )

            OutlinedButton(
                onClick = { scope.launch { Clerk.auth.signOut() } },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Sign Out")
            }

            Text(
                "Asterion v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showReaderSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showReaderSettings = false }, sheetState = sheetState) {
            ReaderSettingsSheet(
                settings = readerSettings,
                onThemeSelected = { theme -> scope.launch { readerPreferences.setTheme(theme) } },
                onFontSelected = { font -> scope.launch { readerPreferences.setFont(font) } },
                onFontSizeChanged = { size -> scope.launch { readerPreferences.setFontSize(size) } },
            )
        }
    }
}

/** A left-aligned identity row (avatar + name/email inline) rather than a boxed, centered card -
 * the old version doubled up on the same surfaceVariant fill as the stat cards right below it and
 * added a redundant "ASTERION PROFILE" eyebrow the top bar's own "Profile" title already said. */
@Composable
private fun ProfileHeader(
    imageUrl: String?,
    initials: String,
    displayName: String,
    email: String?,
    memberSinceYear: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (imageUrl.isNullOrBlank()) {
            AvatarInitials(initials)
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            email?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            memberSinceYear?.let {
                Text(
                    "Member since $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    settings: AppSettings,
    onThemeSelected: (AppThemeMode) -> Unit,
    onFontSelected: (ReaderFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                "Theme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            AppThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onThemeSelected(mode) },
                    label = { Text(mode.displayName()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
            Icon(Icons.Filled.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                "App Font",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            ReaderFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.font == font,
                    onClick = { onFontSelected(font) },
                    label = { Text(font.displayName()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

private fun AppThemeMode.displayName(): String = when (this) {
    AppThemeMode.SYSTEM -> "System"
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
}

/**
 * Bookmarks (novels via /me/library, anime/movies via /me/media/bookmarks) and watch/reading
 * progress now sync to the same account AsterionMac uses - football isn't tracked on either
 * platform (Mac's own model has no football case for playback progress), so it's intentionally
 * absent here too, not a gap specific to Android.
 */
@Composable
private fun SyncSection(
    isLoading: Boolean,
    failed: Boolean,
    savedNovels: Int?,
    stats: MediaAccountStats?,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsterionLoadingIndicator(modifier = Modifier.size(22.dp))
            Text(
                "Syncing…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        failed -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp).size(20.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Couldn't sync right now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Check your connection - saved items and progress will sync again next time you open Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
            StatCard(
                icon = Icons.Filled.AutoStories,
                value = savedNovels ?: 0,
                label = "Saved novels",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Bookmark,
                value = (stats?.savedAnime ?: 0) + (stats?.savedMovies ?: 0),
                label = "Saved anime & movies",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.PlayCircle,
                value = stats?.titlesInProgress ?: 0,
                label = "In progress",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
        }
        Text(
            "$value",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun ProfileCardRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AvatarInitials(initials: String) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
