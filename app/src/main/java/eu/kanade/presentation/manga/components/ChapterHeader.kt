package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChapterHeader(
    enabled: Boolean,
    chapterCount: Int?,
    missingChapterCount: Int,
    chapters: List<ChapterList.Item>,
    onFilterClick: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    canDownloadChapter: (ChapterList.Item) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var showActionsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var chapterNumber by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<ChapterSearchError?>(null) }
    var searchResultIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    val searchResults = remember(chapters, searchResultIds) {
        searchResultIds.mapNotNull { id -> chapters.firstOrNull { it.id == id } }
    }

    // KMK -->
    val openSearchDialog = {
        showActionsDialog = false
        chapterNumber = ""
        searchError = null
        showSearchDialog = true
    }
    // KMK <--

    if (showActionsDialog) {
        AlertDialog(
            onDismissRequest = { showActionsDialog = false },
            title = { Text(stringResource(MR.strings.chapter_list_actions)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showActionsDialog = false
                            onFilterClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                    TextButton(
                        // KMK -->
                        onClick = openSearchDialog,
                        // KMK <--
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(MR.strings.search_chapter))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionsDialog = false }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text(stringResource(MR.strings.search_chapter)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chapterNumber,
                        onValueChange = {
                            chapterNumber = it
                            searchError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(MR.strings.show_chapter_number)) },
                        singleLine = true,
                        // KMK -->
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        // KMK <--
                    )
                    val errorResource = when (searchError) {
                        ChapterSearchError.INVALID_NUMBER -> MR.strings.local_invalid_format
                        ChapterSearchError.NOT_FOUND -> MR.strings.chapter_not_found
                        null -> null
                    }
                    errorResource?.let {
                        Text(
                            text = stringResource(it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = chapterNumber.isNotBlank(),
                    onClick = {
                        // KMK -->
                        val chapterRange = parseChapterSearchRange(chapterNumber)
                        if (chapterRange == null) {
                            searchError = ChapterSearchError.INVALID_NUMBER
                        } else {
                            val matchingChapters = chapters.filter { item ->
                                val number = item.chapter.chapterNumber
                                number.isFinite() && number in chapterRange
                            }
                            if (matchingChapters.isEmpty()) {
                                searchError = ChapterSearchError.NOT_FOUND
                            } else {
                                searchResultIds = matchingChapters.map { it.id }
                                searchError = null
                                showSearchDialog = false
                            }
                        }
                        // KMK <--
                    },
                ) {
                    Text(stringResource(MR.strings.action_search))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    if (searchResults.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { searchResultIds = emptyList() },
            title = { Text(stringResource(MR.strings.chapter_search_results)) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(
                        items = searchResults,
                        key = { item -> "chapter-search-result-${item.id}" },
                    ) { item ->
                        val downloadEnabled = onDownloadChapter != null && canDownloadChapter(item)
                        MangaChapterListItem(
                            title = item.chapter.name,
                            date = null,
                            readProgress = null,
                            scanlator = item.chapter.scanlator.takeIf {
                                !it.isNullOrBlank() && item.showScanlator
                            },
                            sourceName = item.sourceName,
                            read = item.chapter.read,
                            bookmark = item.chapter.bookmark,
                            selected = false,
                            downloadIndicatorEnabled = downloadEnabled,
                            downloadStateProvider = { item.downloadState },
                            downloadProgressProvider = { item.downloadProgress },
                            chapterSwipeStartAction = tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction.Disabled,
                            chapterSwipeEndAction = tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction.Disabled,
                            onLongClick = {},
                            onClick = {
                                searchResultIds = emptyList()
                                onOpenChapter(item.chapter)
                            },
                            onDownloadClick = if (downloadEnabled) {
                                { action -> onDownloadChapter?.invoke(listOf(item), action) }
                            } else {
                                null
                            },
                            onChapterSwipe = {},
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { searchResultIds = emptyList() }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = { showActionsDialog = true },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            Text(
                text = if (chapterCount == null) {
                    stringResource(MR.strings.chapters)
                } else {
                    pluralStringResource(MR.plurals.manga_num_chapters, count = chapterCount, chapterCount)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            MissingChaptersWarning(missingChapterCount)
        }

        // KMK -->
        IconButton(
            enabled = enabled,
            onClick = openSearchDialog,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(MR.strings.action_search),
            )
        }
        // KMK <--
    }
}

// KMK -->
private fun parseChapterSearchRange(input: String): ClosedFloatingPointRange<Double>? {
    val normalized = input.trim()
        .replace('–', '-')
        .replace('—', '-')
        .replace('−', '-')
        .replace(Regex("""(?i)^(?:de|entre|from|between)\s+"""), "")
        .replace(
            Regex("""(?i)\s*(?:tiret|trait\s+d['’]union|jusqu['’]à|à|au|et|to|through|until)\s*"""),
            "-",
        )
        .replace(Regex("""\s+"""), "")

    val numberPattern = """-?\d+(?:[.,]\d+)?"""
    val rangeMatch = Regex("""^($numberPattern)-($numberPattern)$""").matchEntire(normalized)
    val startText: String
    val endText: String

    if (rangeMatch != null) {
        startText = rangeMatch.groupValues[1]
        endText = rangeMatch.groupValues[2]
    } else if (Regex("""^$numberPattern$""").matches(normalized)) {
        startText = normalized
        endText = normalized
    } else {
        return null
    }

    val start = startText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val end = endText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    if (start > end) return null

    return start..end
}
// KMK <--

private enum class ChapterSearchError {
    INVALID_NUMBER,
    NOT_FOUND,
}

@Composable
private fun MissingChaptersWarning(count: Int) {
    if (count == 0) {
        return
    }

    Text(
        text = pluralStringResource(MR.plurals.missing_chapters, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = SECONDARY_ALPHA),
    )
}
