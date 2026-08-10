package cloud.cyberverse.asterion.ui.novels

import cloud.cyberverse.asterion.ui.components.PhosphorIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.cyberverse.asterion.data.model.Chapter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private val paragraphSplitRegex = Regex("</p>|<br\\s*/?>")
private val htmlTagRegex = Regex("<[^>]+>")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterReaderScreen(
    novelId: String,
    chapterNumber: Int,
    onNavigateBack: () -> Unit = {},
    onNavigateToChapter: (Int) -> Unit = {},
    viewModel: ChapterReaderViewModel = koinViewModel(parameters = { parametersOf(novelId, chapterNumber) }),
    readerPreferences: ReaderPreferences = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val settings by readerPreferences.settings.collectAsState(initial = ReaderSettings())
    val palette = settings.theme.palette()
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var showChapterPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapter $chapterNumber", color = palette.text) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(PhosphorIcons.ArrowBack, contentDescription = "Back", tint = palette.text)
                    }
                },
                actions = {
                    IconButton(onClick = { showChapterPicker = true }) {
                        Icon(PhosphorIcons.List, contentDescription = "Chapters", tint = palette.text)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(PhosphorIcons.Settings, contentDescription = "Reader settings", tint = palette.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background),
            )
        },
        bottomBar = {
            val loaded = state as? ChapterReaderState.Loaded
            val chapters = loaded?.allChapters.orEmpty()
            val currentIndex = chapters.indexOfFirst { it.chapterNumber == chapterNumber }
            val previous = chapters.getOrNull(currentIndex - 1)
            val next = chapters.getOrNull(currentIndex + 1)
            if (chapters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.background)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { previous?.let { onNavigateToChapter(it.chapterNumber) } },
                        enabled = previous != null,
                    ) {
                        Icon(PhosphorIcons.ChevronLeft, contentDescription = null, tint = palette.text)
                        Text("Previous", color = palette.text)
                    }
                    TextButton(
                        onClick = { next?.let { onNavigateToChapter(it.chapterNumber) } },
                        enabled = next != null,
                    ) {
                        Text("Next", color = palette.text)
                        Icon(PhosphorIcons.ChevronRight, contentDescription = null, tint = palette.text)
                    }
                }
            }
        },
        containerColor = palette.background,
    ) { padding ->
        when (val current = state) {
            is ChapterReaderState.Loading -> Box(
                Modifier.fillMaxSize().background(palette.background).padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            // Deliberately not the shared ErrorState: the reader runs on its own paper/sepia/ink
            // palette rather than the app theme, so themed colours would clash with the page.
            is ChapterReaderState.Error -> Box(
                Modifier.fillMaxSize().background(palette.background).padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text(current.message, color = palette.text, textAlign = TextAlign.Center)
                    TextButton(onClick = viewModel::load) {
                        Text("Try again", color = palette.text)
                    }
                }
            }

            is ChapterReaderState.Loaded -> {
                val paragraphs = (current.chapter.content ?: "")
                    .split(paragraphSplitRegex)
                    .map { it.replace(htmlTagRegex, "").trim() }
                    .filter { it.isNotEmpty() }

                val scrollState = rememberScrollState()
                LaunchedEffect(scrollState, current.chapter.id) {
                    snapshotFlow { scrollState.value }.collect { value ->
                        viewModel.reportProgress(value, scrollState.maxValue)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        current.chapter.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = settings.font.fontFamily(),
                        color = palette.text,
                    )
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            fontFamily = settings.font.fontFamily(),
                            color = palette.text,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * 1.65f).sp,
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState) {
            ReaderSettingsSheet(
                settings = settings,
                onThemeSelected = { theme -> scope.launch { readerPreferences.setTheme(theme) } },
                onFontSelected = { font -> scope.launch { readerPreferences.setFont(font) } },
                onFontSizeChanged = { size -> scope.launch { readerPreferences.setFontSize(size) } },
            )
        }
    }

    if (showChapterPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val chapters = (state as? ChapterReaderState.Loaded)?.allChapters.orEmpty()
        ModalBottomSheet(onDismissRequest = { showChapterPicker = false }, sheetState = sheetState) {
            ChapterPickerSheet(
                chapters = chapters,
                currentChapterNumber = chapterNumber,
                palette = palette,
                onChapterSelected = { number ->
                    showChapterPicker = false
                    onNavigateToChapter(number)
                },
            )
        }
    }
}

@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onThemeSelected: (ReaderTheme) -> Unit,
    onFontSelected: (ReaderFont) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
        Text("Reading settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Text("Theme", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReaderTheme.entries.forEach { theme ->
                val palette = theme.palette()
                val isSelected = settings.theme == theme
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.background)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else palette.border,
                            shape = CircleShape,
                        )
                        .clickable { onThemeSelected(theme) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = palette.text, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Font", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.font == font,
                    onClick = { onFontSelected(font) },
                    label = { Text(font.displayName(), fontFamily = font.fontFamily()) },
                )
            }
        }

        Text("Font size", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", fontSize = 14.sp)
            Slider(
                value = settings.fontSizeSp,
                onValueChange = onFontSizeChanged,
                valueRange = 14f..30f,
                steps = 15,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("A", fontSize = 26.sp)
        }
    }
}

@Composable
private fun ChapterPickerSheet(
    chapters: List<Chapter>,
    currentChapterNumber: Int,
    palette: ReaderPalette,
    onChapterSelected: (Int) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(chapters, query) {
        if (query.isBlank()) {
            chapters
        } else {
            chapters.filter {
                it.title.contains(query, ignoreCase = true) || it.chapterNumber.toString() == query.trim()
            }
        }
    }
    val listState = rememberLazyListState()
    val currentIndex = chapters.indexOfFirst { it.chapterNumber == currentChapterNumber }

    LaunchedEffect(chapters) {
        if (currentIndex >= 0) listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxWidth().height(480.dp).padding(horizontal = 16.dp)) {
        Text(
            "${chapters.size} chapters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by title or number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            items(filtered, key = { it.id }) { chapter ->
                val isCurrent = chapter.chapterNumber == currentChapterNumber
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { onChapterSelected(chapter.chapterNumber) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${chapter.chapterNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(chapter.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
